package com.andrew.smartielts.auth.mapper;

import com.andrew.smartielts.auth.domain.pojo.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthMapper {

    User findByEmail(String email);

    Boolean existsByEmail(String email);

    void save(User user);
}
