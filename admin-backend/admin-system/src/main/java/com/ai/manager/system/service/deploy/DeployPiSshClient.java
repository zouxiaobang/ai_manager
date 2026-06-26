package com.ai.manager.system.service.deploy;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Component
public class DeployPiSshClient {

    private final String user;
    private final String host;
    private final String password;

    public DeployPiSshClient(
            @Value("${ai-manager.deploy.pi.user:kyle}") String user,
            @Value("${ai-manager.deploy.pi.host:192.168.0.114}") String host,
            @Value("${ai-manager.deploy.pi.password:}") String password) {
        this.user = user == null ? "kyle" : user.trim();
        this.host = host == null ? "192.168.0.114" : host.trim();
        this.password = password == null ? "" : password.trim();
    }

    public boolean isPasswordAuthEnabled() {
        return !password.isBlank();
    }

    public String targetLabel() {
        return user + "@" + host;
    }

    public void testConnection() throws IOException {
        try (SSHClient ssh = openClient()) {
            // connected
        }
    }

    public String execute(String command) throws IOException {
        try (SSHClient ssh = openClient()) {
            try (Session session = ssh.startSession()) {
                Session.Command cmd = session.exec(command);
                cmd.join(5, TimeUnit.MINUTES);
                String stdout = readStream(cmd.getInputStream());
                String stderr = readStream(cmd.getErrorStream());
                if (cmd.getExitStatus() != null && cmd.getExitStatus() != 0) {
                    String detail = stderr.isBlank() ? stdout : stderr;
                    throw new IOException(
                            "远程命令失败 (exit " + cmd.getExitStatus() + "): " + detail.trim());
                }
                if (!stderr.isBlank() && stdout.isBlank()) {
                    return stderr.trim();
                }
                return stdout.trim();
            }
        }
    }

    public void uploadFile(Path localFile, String remotePath) throws IOException {
        if (!Files.isRegularFile(localFile)) {
            throw new IOException("本地文件不存在: " + localFile);
        }
        String remoteDir = remoteParent(remotePath);
        if (!remoteDir.equals("/tmp")) {
            execute("mkdir -p " + shellQuote(remoteDir));
        }
        try (SSHClient ssh = openClient()) {
            try (SFTPClient sftp = ssh.newSFTPClient()) {
                sftp.put(new FileSystemFile(localFile.toFile()), remotePath);
            } catch (IOException ex) {
                throw new IOException(
                        "上传失败 " + remotePath + "（若目标在 /opt 下，请改用 uploadAndInstallBackendJar）: "
                                + ex.getMessage(),
                        ex);
            }
        }
    }

    public void uploadAndInstallBackendJar(Path localJar, String backendDir) throws IOException {
        String stagingPath = "/tmp/admin-server.jar";
        String targetPath = backendDir.endsWith("/")
                ? backendDir + "admin-server.jar"
                : backendDir + "/admin-server.jar";
        uploadFile(localJar, stagingPath);
        execute(
                "sudo mv "
                        + shellQuote(stagingPath)
                        + " "
                        + shellQuote(targetPath)
                        + " && sudo chown aimanager:aimanager "
                        + shellQuote(targetPath));
    }

    public void uploadDirectory(Path localDir, String remoteDir) throws IOException {
        if (!Files.isDirectory(localDir)) {
            throw new IOException("本地目录不存在: " + localDir);
        }
        String remoteRoot = remoteDir.endsWith("/") ? remoteDir.substring(0, remoteDir.length() - 1) : remoteDir;
        execute("rm -rf " + shellQuote(remoteRoot) + " && mkdir -p " + shellQuote(remoteRoot));

        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(localDir)) {
            walk.filter(Files::isRegularFile).forEach(files::add);
        }
        if (files.isEmpty()) {
            throw new IOException("本地目录为空: " + localDir);
        }

        try (SSHClient ssh = openClient()) {
            try (SFTPClient sftp = ssh.newSFTPClient()) {
                for (Path path : files) {
                    String relative = localDir.relativize(path).toString().replace('\\', '/');
                    String remotePath = remoteRoot + "/" + relative;
                    ensureRemoteParentDirs(sftp, remotePath);
                    try {
                        sftp.put(new FileSystemFile(path.toFile()), remotePath);
                    } catch (IOException ex) {
                        throw new IOException(
                                "上传失败 " + relative + " -> " + remotePath + ": " + ex.getMessage(),
                                ex);
                    }
                }
            }
        }
    }

    private void ensureRemoteParentDirs(SFTPClient sftp, String remoteFilePath) throws IOException {
        String parent = remoteParent(remoteFilePath);
        if (parent.isBlank() || "/".equals(parent)) {
            return;
        }
        StringBuilder built = new StringBuilder();
        for (String segment : parent.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            built.append("/").append(segment);
            try {
                sftp.mkdir(built.toString());
            } catch (IOException ignored) {
                // 目录已存在时忽略
            }
        }
    }

    private SSHClient openClient() throws IOException {
        if (!isPasswordAuthEnabled()) {
            throw new IOException("未配置树莓派 SSH 密码（ai-manager.deploy.pi.password）");
        }
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.setConnectTimeout(20_000);
        ssh.setTimeout(10 * 60_000);
        ssh.connect(host, 22);
        ssh.authPassword(user, password);
        return ssh;
    }

    private static String remoteParent(String remotePath) {
        int index = remotePath.lastIndexOf('/');
        if (index <= 0) {
            return "/";
        }
        return remotePath.substring(0, index);
    }

    public static String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private static String readStream(java.io.InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
}
