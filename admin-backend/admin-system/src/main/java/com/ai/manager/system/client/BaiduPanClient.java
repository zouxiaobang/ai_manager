package com.ai.manager.system.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaiduPanClient {

    private static final String TOKEN_URL = "https://openapi.baidu.com/oauth/2.0/token";
    private static final String XPAN_FILE = "https://pan.baidu.com/rest/2.0/xpan/file";
    private static final String XPAN_MULTIMEDIA = "https://pan.baidu.com/rest/2.0/xpan/multimedia";
    private static final String PCS_LOCATE_UPLOAD = "https://d.pcs.baidu.com/rest/2.0/pcs/file";
    private static final String DEFAULT_UPLOAD_HOST = "https://c3.pcs.baidu.com";
    private static final int PCS_APP_ID = 250528;
    private static final String BAIDU_DOWNLOAD_USER_AGENT = "pan.baidu.com";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public BaiduTokenResponse exchangeCode(String appKey, String secretKey, String redirectUri, String code)
            throws IOException, InterruptedException {
        String query = buildQuery(Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "client_id", appKey,
                "client_secret", secretKey,
                "redirect_uri", redirectUri
        ));
        return requestToken(query);
    }

    public BaiduTokenResponse refreshToken(String appKey, String secretKey, String refreshToken)
            throws IOException, InterruptedException {
        String query = buildQuery(Map.of(
                "grant_type", "refresh_token",
                "refresh_token", refreshToken,
                "client_id", appKey,
                "client_secret", secretKey
        ));
        return requestToken(query);
    }

    /**
     * 官方三步上传：precreate(rtype=3 覆盖) → superfile2 → create
     */
    public BaiduUploadResponse upload(String accessToken, String path, byte[] content)
            throws IOException, InterruptedException {
        byte[] payload = normalizeContent(content);
        String md5 = md5Hex(payload);
        String blockListJson = "[\"" + md5 + "\"]";

        JsonNode precreate = precreate(accessToken, path, payload.length, blockListJson);
        int returnType = precreate.path("return_type").asInt(0);
        if (returnType == 2) {
            return toUploadResponse(precreate, path, payload.length);
        }

        String uploadId = precreate.path("uploadid").asText("");
        if (!StringUtils.hasText(uploadId)) {
            throw new IOException("百度网盘预上传未返回 uploadid: " + precreate);
        }

        String uploadHost = locateUploadHost(accessToken, path, uploadId);
        uploadSlice(uploadHost, accessToken, path, uploadId, 0, payload);

        JsonNode created = createFile(accessToken, path, payload.length, uploadId, blockListJson);
        return toUploadResponse(created, path, payload.length);
    }

    public String download(String accessToken, String path, Long fsId) throws IOException, InterruptedException {
        Long resolvedFsId = fsId;
        if (resolvedFsId == null) {
            resolvedFsId = findFsIdByPath(accessToken, path);
        }
        if (resolvedFsId == null) {
            return "";
        }
        String metaUrl = XPAN_MULTIMEDIA + "?method=filemetas&access_token=" + url(accessToken)
                + "&fsids=[" + resolvedFsId + "]&dlink=1";
        HttpRequest metaRequest = HttpRequest.newBuilder(URI.create(metaUrl))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> metaResponse = httpClient.send(metaRequest, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(metaResponse.body());
        checkErrno(root);
        JsonNode list = root.path("list");
        if (!list.isArray() || list.isEmpty()) {
            return "";
        }
        String dlink = list.get(0).path("dlink").asText("");
        if (!StringUtils.hasText(dlink)) {
            return "";
        }
        dlink = dlink.replace("\\u0026", "&");
        String downloadUrl = dlink + (dlink.contains("?") ? "&" : "?") + "access_token=" + url(accessToken);
        HttpRequest downloadRequest = HttpRequest.newBuilder(URI.create(downloadUrl))
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", BAIDU_DOWNLOAD_USER_AGENT)
                .GET()
                .build();
        HttpResponse<byte[]> downloadResponse = httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofByteArray());
        if (downloadResponse.statusCode() >= 400) {
            throw new IOException("下载百度网盘文件失败: HTTP " + downloadResponse.statusCode());
        }
        String body = new String(downloadResponse.body(), StandardCharsets.UTF_8);
        validateDownloadBody(body);
        return body;
    }

    private void validateDownloadBody(String body) throws IOException {
        if (!StringUtils.hasText(body)) {
            return;
        }
        String trimmed = body.trim();
        if (!trimmed.startsWith("{")) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(trimmed);
            if (root.has("error_code")) {
                int errorCode = root.path("error_code").asInt(0);
                if (errorCode != 0) {
                    throw new IOException("百度网盘下载失败: error_code=" + errorCode
                            + ", request_id=" + root.path("request_id").asText(""));
                }
            }
            if (root.has("errno") && root.path("errno").asInt(0) != 0) {
                throw new IOException("百度网盘下载失败: errno=" + root.path("errno").asInt()
                        + ", request_id=" + root.path("request_id").asText(""));
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception ignored) {
            // 非百度错误 JSON，按正文处理
        }
    }

    public void delete(String accessToken, String path) throws IOException, InterruptedException {
        String fileList = objectMapper.writeValueAsString(new Object[]{Map.of("path", path)});
        String requestUrl = XPAN_FILE + "?method=filemanager&access_token=" + url(accessToken)
                + "&opera=delete&async=0";
        HttpRequest request = HttpRequest.newBuilder(URI.create(requestUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("filelist=" + url(fileList)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("删除百度网盘文件失败: HTTP " + response.statusCode() + ", body=" + response.body());
        }
        JsonNode root = objectMapper.readTree(response.body());
        checkErrno(root);
        if (root.has("info") && root.path("info").isArray()) {
            for (JsonNode item : root.path("info")) {
                int errno = item.path("errno").asInt(0);
                if (errno != 0) {
                    throw new IOException("删除百度网盘文件失败: errno=" + errno + ", path=" + path);
                }
            }
        }
    }

    public void ensureDir(String accessToken, String dirPath) throws IOException, InterruptedException {
        String requestUrl = XPAN_FILE + "?method=create&access_token=" + url(accessToken);
        // rtype=2：目录已存在则忽略，避免 rtype=1 每次重命名为 notes_YYYYMMDD_HHMMSS
        String body = "path=" + encodePath(dirPath) + "&isdir=1&rtype=2";
        HttpRequest request = HttpRequest.newBuilder(URI.create(requestUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        int errno = root.path("errno").asInt(0);
        if (errno == 0 || errno == -8) {
            return;
        }
        throw new IOException("创建百度网盘目录失败: errno=" + errno + ", body=" + response.body());
    }

    private JsonNode precreate(String accessToken, String path, int size, String blockListJson)
            throws IOException, InterruptedException {
        String requestUrl = XPAN_FILE + "?method=precreate&access_token=" + url(accessToken);
        String body = "path=" + encodePath(path)
                + "&size=" + size
                + "&isdir=0"
                + "&autoinit=1"
                + "&rtype=3"
                + "&block_list=" + url(blockListJson);
        return postForm(requestUrl, body);
    }

    private JsonNode createFile(String accessToken, String path, int size, String uploadId, String blockListJson)
            throws IOException, InterruptedException {
        String requestUrl = XPAN_FILE + "?method=create&access_token=" + url(accessToken);
        String body = "path=" + encodePath(path)
                + "&size=" + size
                + "&isdir=0"
                + "&rtype=3"
                + "&uploadid=" + url(uploadId)
                + "&block_list=" + url(blockListJson);
        return postForm(requestUrl, body);
    }

    private String locateUploadHost(String accessToken, String path, String uploadId)
            throws IOException, InterruptedException {
        String requestUrl = PCS_LOCATE_UPLOAD + "?method=locateupload"
                + "&appid=" + PCS_APP_ID
                + "&access_token=" + url(accessToken)
                + "&path=" + encodePath(path)
                + "&uploadid=" + url(uploadId)
                + "&upload_version=2.0";
        HttpRequest request = HttpRequest.newBuilder(URI.create(requestUrl))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        checkErrno(root);
        if (root.path("servers").isArray() && !root.path("servers").isEmpty()) {
            String server = root.path("servers").get(0).path("server").asText("");
            if (StringUtils.hasText(server)) {
                return server.startsWith("http") ? server : "https://" + server;
            }
        }
        String host = root.path("host").asText("");
        if (StringUtils.hasText(host)) {
            return host.startsWith("http") ? host : "https://" + host;
        }
        return DEFAULT_UPLOAD_HOST;
    }

    private void uploadSlice(String uploadHost, String accessToken, String path, String uploadId, int partSeq,
                               byte[] content) throws IOException, InterruptedException {
        String base = uploadHost.endsWith("/") ? uploadHost.substring(0, uploadHost.length() - 1) : uploadHost;
        String requestUrl = base + "/rest/2.0/pcs/superfile2?method=upload"
                + "&access_token=" + url(accessToken)
                + "&type=tmpfile"
                + "&path=" + encodePath(path)
                + "&uploadid=" + url(uploadId)
                + "&partseq=" + partSeq;
        String fileName = fileNameFromPath(path);
        String boundary = "----BaiduPan" + UUID.randomUUID().toString().replace("-", "");
        byte[] body = buildMultipartFileBody(content, fileName, boundary);
        HttpRequest request = HttpRequest.newBuilder(URI.create(requestUrl))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();
        if (response.statusCode() >= 400 || containsApiError(responseBody)) {
            throw new IOException("百度网盘分片上传失败: HTTP " + response.statusCode() + ", body=" + responseBody);
        }
        JsonNode root = objectMapper.readTree(responseBody);
        checkErrno(root);
    }

    private JsonNode postForm(String requestUrl, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(requestUrl))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();
        if (response.statusCode() >= 400 || containsApiError(responseBody)) {
            throw new IOException("百度网盘 API 请求失败: HTTP " + response.statusCode() + ", body=" + responseBody);
        }
        JsonNode root = objectMapper.readTree(responseBody);
        checkErrno(root);
        return root;
    }

    private BaiduUploadResponse toUploadResponse(JsonNode root, String path, long size) {
        BaiduUploadResponse result = new BaiduUploadResponse();
        result.setPath(root.path("path").asText(path));
        if (root.has("fs_id")) {
            result.setFsId(root.path("fs_id").asLong());
        }
        result.setSize(root.path("size").asLong(size));
        return result;
    }

    private byte[] normalizeContent(byte[] content) {
        if (content == null || content.length == 0) {
            return "<p></p>".getBytes(StandardCharsets.UTF_8);
        }
        return content;
    }

    private String md5Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }

    private Long findFsIdByPath(String accessToken, String path) throws IOException, InterruptedException {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            return null;
        }
        String dir = path.substring(0, lastSlash);
        String fileName = path.substring(lastSlash + 1);
        String requestUrl = XPAN_FILE + "?method=list&access_token=" + url(accessToken)
                + "&dir=" + encodePath(dir) + "&limit=1000";
        HttpRequest request = HttpRequest.newBuilder(URI.create(requestUrl))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        checkErrno(root);
        for (JsonNode item : root.path("list")) {
            if (fileName.equals(item.path("server_filename").asText())) {
                return item.path("fs_id").asLong();
            }
        }
        return null;
    }

    private BaiduTokenResponse requestToken(String query) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_URL + "?" + query))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        BaiduTokenResponse token = objectMapper.readValue(response.body(), BaiduTokenResponse.class);
        if (!StringUtils.hasText(token.getAccessToken())) {
            throw new IOException("获取百度 token 失败: " + response.body());
        }
        return token;
    }

    private boolean containsApiError(String responseBody) throws IOException {
        if (!StringUtils.hasText(responseBody)) {
            return false;
        }
        JsonNode root = objectMapper.readTree(responseBody);
        if (root.has("error_code") && root.path("error_code").asInt(0) != 0) {
            return true;
        }
        return root.has("errno") && root.path("errno").asInt(0) != 0;
    }

    private byte[] buildMultipartFileBody(byte[] content, String fileName, String boundary) {
        String partHeader = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        String partFooter = "\r\n--" + boundary + "--\r\n";
        byte[] headerBytes = partHeader.getBytes(StandardCharsets.UTF_8);
        byte[] footerBytes = partFooter.getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[headerBytes.length + content.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(content, 0, body, headerBytes.length, content.length);
        System.arraycopy(footerBytes, 0, body, headerBytes.length + content.length, footerBytes.length);
        return body;
    }

    private String fileNameFromPath(String path) {
        int index = path.lastIndexOf('/');
        if (index >= 0 && index < path.length() - 1) {
            return path.substring(index + 1);
        }
        return "note.html";
    }

    private void checkErrno(JsonNode root) throws IOException {
        if (root.has("error_code")) {
            int errorCode = root.path("error_code").asInt(0);
            if (errorCode != 0) {
                throw new IOException("百度网盘 API 错误: error_code=" + errorCode
                        + ", error_msg=" + root.path("error_msg").asText("") + ", body=" + root);
            }
        }
        if (!root.has("errno")) {
            return;
        }
        int errno = root.path("errno").asInt(0);
        if (errno != 0) {
            throw new IOException("百度网盘 API 错误: errno=" + errno + ", body=" + root);
        }
    }

    private String buildQuery(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((key, value) -> {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(url(key)).append('=').append(url(value));
        });
        return sb.toString();
    }

    private String encodePath(String path) {
        return URLEncoder.encode(path, StandardCharsets.UTF_8);
    }

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BaiduTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private long expiresIn;

        @JsonProperty("error")
        private String error;

        @JsonProperty("error_description")
        private String errorDescription;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BaiduUploadResponse {
        private String path;

        @JsonProperty("fs_id")
        private Long fsId;

        private Long size;
    }
}
