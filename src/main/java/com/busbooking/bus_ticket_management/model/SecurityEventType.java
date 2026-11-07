package com.busbooking.bus_ticket_management.model;


/** Account-only events; excludes ticket, revenue and trip activity. */
public enum SecurityEventType {
    EMAIL_VERIFIED("Xác thực email đăng ký"),
    EMAIL_CHANGE_REQUESTED("Yêu cầu đổi email"),
    EMAIL_CHANGED("Xác nhận email mới"),
    EMAIL_CHANGE_CANCELLED("Hủy yêu cầu đổi email"),
    REGISTERED("Đăng ký tài khoản"),
    LOGIN_SUCCESS("Đăng nhập thành công"),
    LOGIN_FAILED("Đăng nhập thất bại"),
    LOGIN_BLOCKED("Tạm chặn đăng nhập do nhập sai nhiều lần"),
    LOGOUT("Đăng xuất"),
    PROFILE_UPDATED("Cập nhật hồ sơ"),
    PASSWORD_CHANGED("Đổi mật khẩu"),
    PASSWORD_RESET_REQUESTED("Yêu cầu đặt lại mật khẩu"),
    PASSWORD_RESET("Đặt lại mật khẩu"),
    SESSION_REVOKED("Thu hồi một phiên đăng nhập"),
    OTHER_SESSIONS_REVOKED("Đăng xuất các phiên khác"),
    ALL_SESSIONS_REVOKED("Đăng xuất tất cả phiên");

    private final String label;

    SecurityEventType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
