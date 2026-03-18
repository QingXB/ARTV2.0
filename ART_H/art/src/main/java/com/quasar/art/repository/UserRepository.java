package com.quasar.art.repository;

import com.quasar.art.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ==================== 登录用的查询方法 ====================
    // 根据字段把整条用户数据查出来
    User findByUsername(String username);
    User findByEmail(String email);
    User findByPhone(String phone);

    // ==================== 注册用的查重方法 (防撞车) ====================
    // 🌟 JPA 魔法：只要你的方法名以 "existsBy" 开头，加上实体类里的属性名
    // 它就会自动执行一条极速的 SELECT COUNT(*) 语句，并返回 true(已存在) 或 false(不存在)
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhone(String phone);
}