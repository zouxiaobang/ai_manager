package com.ai.manager.system.service.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AI 知识库配置 JSON 的落库加密（AES-GCM）
 *
 * <p>解决 API Key 在 {@code ai_knowledge_config.config_json} 明文落库的问题：
 * 写入时加密、读取时解密，库中不再出现明文密钥。存储格式：
 * {@code enc:v1:<base64(iv||ciphertext||tag)>}，前缀标记用于区分已加密值与历史明文。</p>
 *
 * <p>主密钥来自环境变量 {@code AI_MANAGER_CONFIG_MASTER_KEY}，经 SHA-256 派生为 256 位 AES 密钥。
 * 未配置时使用内置开发密钥并输出警告——生产环境必须设置，否则加密形同虚设（同一主密钥才能解密）。</p>
 */
@Slf4j
@Component
public class ConfigCryptoService {

    /** 加密值前缀标记：以该前缀开头视为已加密 */
    public static final String ENC_PREFIX = "enc:v1:";

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String ENV_MASTER_KEY = "AI_MANAGER_CONFIG_MASTER_KEY";
    /** 开发用内置密钥（仅当环境变量未配置时使用；SHA-256 派生为 256 位密钥） */
    private static final String DEV_DEFAULT_KEY = "dev-config-master-key-change-me-in-prod";

    private final SecretKeySpec key;

    /** Spring 构造：主密钥来自环境变量 AI_MANAGER_CONFIG_MASTER_KEY，缺省回退内置开发密钥 */
    public ConfigCryptoService() {
        this(readMasterKeyFromEnv());
    }

    /** 供测试等程序化场景直接指定主密钥构造（不依赖环境变量） */
    ConfigCryptoService(String masterKey) {
        this.key = deriveKey(masterKey);
    }

    public boolean isEncrypted(String json) {
        return json != null && json.startsWith(ENC_PREFIX);
    }

    /** 若为加密值则解密，否则原样返回（兼容历史明文，供迁移期读取） */
    public String decryptIfEncrypted(String json) {
        return isEncrypted(json) ? decrypt(json) : json;
    }

    public String encrypt(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return ENC_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("配置加密失败", e);
        }
    }

    public String decrypt(String ciphertext) {
        try {
            String base64 = ciphertext.startsWith(ENC_PREFIX)
                    ? ciphertext.substring(ENC_PREFIX.length()) : ciphertext;
            byte[] combined = Base64.getDecoder().decode(base64);
            if (combined.length < GCM_IV_LENGTH + GCM_TAG_BITS / 8) {
                throw new IllegalArgumentException("加密数据长度异常");
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            byte[] ciphertextBytes = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertextBytes, 0, ciphertextBytes.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertextBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("配置解密失败（主密钥不匹配或数据损坏）", e);
        }
    }

    private static String readMasterKeyFromEnv() {
        String raw = System.getenv(ENV_MASTER_KEY);
        if (raw == null || raw.isBlank()) {
            log.warn("未配置环境变量 {}，使用内置开发密钥；生产环境必须设置，否则落库加密无效",
                    ENV_MASTER_KEY);
            return DEV_DEFAULT_KEY;
        }
        return raw;
    }

    private static SecretKeySpec deriveKey(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("配置主密钥派生失败", e);
        }
    }
}
