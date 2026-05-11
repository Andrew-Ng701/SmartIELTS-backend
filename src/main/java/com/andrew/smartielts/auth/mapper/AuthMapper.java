package com.andrew.smartielts.auth.mapper;

import com.andrew.smartielts.auth.domain.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthMapper {

    User findActiveByEmail(@Param("email") String email);

    User findAnyByEmail(@Param("email") String email);

    User findActiveById(@Param("id") Long id);

    Boolean existsByEmail(@Param("email") String email);

    void save(User user);

    void updatePasswordById(@Param("id") Long id,
                            @Param("password") String password);

    void updateLastLoginTimeById(@Param("id") Long id);

    /**
     * tokenVersion = tokenVersion + 1
     * 用於 logout / changePassword 後讓舊 token 失效。
     */
    void incrementTokenVersionById(@Param("id") Long id);
}
