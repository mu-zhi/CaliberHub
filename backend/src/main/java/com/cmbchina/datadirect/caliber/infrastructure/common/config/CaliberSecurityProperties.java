package com.cmbchina.datadirect.caliber.infrastructure.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "caliber.security")
public class CaliberSecurityProperties {

    private boolean enabled = true;
    private String jwtSecret = "";
    private long tokenExpireMinutes = 480;
    private boolean requireWriteAuth = true;
    private boolean rateLimitEnabled = true;
    private int writePerMinute = 120;
    private int llmPerMinute = 30;
    private List<UserAccount> users = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getTokenExpireMinutes() {
        return tokenExpireMinutes;
    }

    public void setTokenExpireMinutes(long tokenExpireMinutes) {
        this.tokenExpireMinutes = tokenExpireMinutes;
    }

    public boolean isRequireWriteAuth() {
        return requireWriteAuth;
    }

    public void setRequireWriteAuth(boolean requireWriteAuth) {
        this.requireWriteAuth = requireWriteAuth;
    }

    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }

    public void setRateLimitEnabled(boolean rateLimitEnabled) {
        this.rateLimitEnabled = rateLimitEnabled;
    }

    public int getWritePerMinute() {
        return writePerMinute;
    }

    public void setWritePerMinute(int writePerMinute) {
        this.writePerMinute = writePerMinute;
    }

    public int getLlmPerMinute() {
        return llmPerMinute;
    }

    public void setLlmPerMinute(int llmPerMinute) {
        this.llmPerMinute = llmPerMinute;
    }

    public List<UserAccount> getUsers() {
        return users;
    }

    public void setUsers(List<UserAccount> users) {
        this.users = users;
    }

    @PostConstruct
    public void validate() {
        if (!enabled) {
            return;
        }
        String normalizedSecret = jwtSecret == null ? "" : jwtSecret.trim();
        if (normalizedSecret.isEmpty()) {
            throw new IllegalStateException("caliber.security.jwt-secret 不能为空，请通过环境变量 CALIBER_JWT_SECRET 注入");
        }
        if (users == null || users.isEmpty()) {
            throw new IllegalStateException("caliber.security.users 不能为空，至少需要配置一个账号");
        }
        for (int i = 0; i < users.size(); i++) {
            UserAccount account = users.get(i);
            String index = "caliber.security.users[" + i + "]";
            String username = account == null || account.getUsername() == null ? "" : account.getUsername().trim();
            String password = account == null || account.getPassword() == null ? "" : account.getPassword().trim();
            if (username.isEmpty()) {
                throw new IllegalStateException(index + ".username 不能为空");
            }
            if (password.isEmpty()) {
                throw new IllegalStateException(index + ".password 不能为空，请通过环境变量注入");
            }
            boolean hasValidRole = account.getRoles() != null
                    && account.getRoles().stream()
                    .anyMatch(role -> role != null && !role.trim().isEmpty());
            if (!hasValidRole) {
                throw new IllegalStateException(index + ".roles 不能为空");
            }
        }
    }

    public static class UserAccount {
        private String username;
        private String password;
        private List<String> roles = new ArrayList<>();

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }
}
