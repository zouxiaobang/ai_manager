package com.ai.manager.system.service.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ConfigCryptoService 单测：AES-GCM 加解密、前缀标记、历史明文兼容、错误主密钥与损坏数据。
 */
class ConfigCryptoServiceTest {

    @Test
    void encrypt_decrypt_还原原文() {
        ConfigCryptoService crypto = new ConfigCryptoService("test-master-key");
        String plain = "{\"provider\":\"openai\",\"apiKey\":\"sk-abc123\"}";

        String encrypted = crypto.encrypt(plain);

        assertThat(encrypted).startsWith(ConfigCryptoService.ENC_PREFIX);
        assertThat(crypto.decrypt(encrypted)).isEqualTo(plain);
    }

    @Test
    void encrypt_同明文两次密文不同_GCM随机IV() {
        ConfigCryptoService crypto = new ConfigCryptoService("test-master-key");
        assertThat(crypto.encrypt("same")).isNotEqualTo(crypto.encrypt("same"));
    }

    @Test
    void isEncrypted_识别前缀标记() {
        ConfigCryptoService crypto = new ConfigCryptoService("test-master-key");
        assertThat(crypto.isEncrypted(crypto.encrypt("x"))).isTrue();
        assertThat(crypto.isEncrypted("{\"provider\":\"openai\"}")).isFalse();
        assertThat(crypto.isEncrypted(null)).isFalse();
    }

    @Test
    void decryptIfEncrypted_明文原样返回_密文解密() {
        ConfigCryptoService crypto = new ConfigCryptoService("test-master-key");
        assertThat(crypto.decryptIfEncrypted("plain-json")).isEqualTo("plain-json");
        String encrypted = crypto.encrypt("secret");
        assertThat(crypto.decryptIfEncrypted(encrypted)).isEqualTo("secret");
    }

    @Test
    void decrypt_错误主密钥_抛异常() {
        ConfigCryptoService enc = new ConfigCryptoService("master-key-a");
        ConfigCryptoService dec = new ConfigCryptoService("master-key-b");

        String encrypted = enc.encrypt("payload");

        assertThatThrownBy(() -> dec.decrypt(encrypted))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("解密失败");
    }

    @Test
    void decrypt_损坏数据_抛异常() {
        ConfigCryptoService crypto = new ConfigCryptoService("test-master-key");
        // base64 长度不足（IV+tag），应被拒绝
        assertThatThrownBy(() -> crypto.decrypt(ConfigCryptoService.ENC_PREFIX + "bm90LWVub3VnaA=="))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("解密失败");
    }
}
