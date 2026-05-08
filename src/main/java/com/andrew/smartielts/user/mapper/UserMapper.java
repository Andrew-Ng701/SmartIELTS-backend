package com.andrew.smartielts.user.mapper;

import com.andrew.smartielts.auth.domain.pojo.User;
import com.andrew.smartielts.user.domain.query.admin.AdminDeletedUserPageQuery;
import com.andrew.smartielts.user.domain.query.admin.AdminUserPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    User findAnyById(@Param("id") Long id);

    User findActiveById(@Param("id") Long id);

    Long countActive(@Param("query") AdminUserPageQuery query);

    List<User> pageActive(@Param("query") AdminUserPageQuery query,
                          @Param("offset") Integer offset,
                          @Param("limit") Integer limit);

    Long countDeleted(@Param("query") AdminDeletedUserPageQuery query);

    List<User> pageDeleted(@Param("query") AdminDeletedUserPageQuery query,
                           @Param("offset") Integer offset,
                           @Param("limit") Integer limit);

    Long countAllUsers();

    Long countActiveUsers();

    Long countDeletedUsers();

    void softDeleteById(@Param("id") Long id);

    void restoreById(@Param("id") Long id);

    Boolean existsActiveEmailExcludeId(@Param("email") String email,
                                       @Param("id") Long id);

    void updateEmailById(@Param("id") Long id,
                         @Param("email") String email);
}
