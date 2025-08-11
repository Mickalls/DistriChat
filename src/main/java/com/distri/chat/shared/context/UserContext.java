package com.distri.chat.shared.context;

/**
 * 用户上下文类
 * 存储当前请求的用户信息，使用ThreadLocal确保线程安全
 */
public class UserContext {

    private static final ThreadLocal<UserInfo> CONTEXT = new ThreadLocal<>();

    /**
     * 设置用户信息
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     */
    public static void setUser(Long userId, String deviceId) {
        CONTEXT.set(new UserInfo(userId, deviceId));
    }

    /**
     * 获取当前用户ID
     * 
     * @return 用户ID，如果未设置则返回null
     */
    public static Long getCurrentUserId() {
        UserInfo userInfo = CONTEXT.get();
        return userInfo != null ? userInfo.getUserId() : null;
    }

    /**
     * 获取当前设备ID
     * 
     * @return 设备ID，如果未设置则返回null
     */
    public static String getCurrentDeviceId() {
        UserInfo userInfo = CONTEXT.get();
        return userInfo != null ? userInfo.getDeviceId() : null;
    }

    /**
     * 获取完整的用户信息
     * 
     * @return UserInfo对象，如果未设置则返回null
     */
    public static UserInfo getCurrentUser() {
        return CONTEXT.get();
    }

    /**
     * 检查是否已设置用户信息
     * 
     * @return true-已设置, false-未设置
     */
    public static boolean hasUser() {
        return CONTEXT.get() != null;
    }

    /**
     * 清除当前线程的用户信息
     * 重要：防止内存泄漏，请求结束时必须调用
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 用户信息内部类
     */
    public static class UserInfo {
        private final Long userId;
        private final String deviceId;

        public UserInfo(Long userId, String deviceId) {
            this.userId = userId;
            this.deviceId = deviceId;
        }

        public Long getUserId() {
            return userId;
        }

        public String getDeviceId() {
            return deviceId;
        }

        @Override
        public String toString() {
            return "UserInfo{" +
                    "userId=" + userId +
                    ", deviceId='" + deviceId + '\'' +
                    '}';
        }
    }
}
