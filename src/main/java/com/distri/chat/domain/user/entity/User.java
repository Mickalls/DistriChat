package com.distri.chat.domain.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@TableName("users")
public class User {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 手机号 - 唯一标识
     */
    private String phone;
    
    /**
     * 加密后的密码
     */
    private String password;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 用户状态：ACTIVE, INACTIVE, BANNED
     */
    private String status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Boolean deleted;
    
    // 构造函数
    public User() {}
    
    public User(String phone, String password, String nickname) {
        this.phone = phone;
        this.password = password;
        this.nickname = nickname;
        this.status = "ACTIVE";
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.deleted = false;
    }
    
    // Getter/Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}
