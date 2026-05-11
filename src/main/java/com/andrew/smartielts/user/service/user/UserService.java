package com.andrew.smartielts.user.service.user;

import com.andrew.smartielts.dashboard.domain.vo.UserOverviewVO;
import com.andrew.smartielts.user.domain.dto.UserProfileUpdateDTO;
import com.andrew.smartielts.user.domain.vo.UserProfileVO;
import com.andrew.smartielts.user.domain.vo.UserStatsVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserProfileVO getProfile();

    UserProfileVO updateProfile(UserProfileUpdateDTO dto);

    UserProfileVO getProfilePicture();

    UserProfileVO updateProfilePicture(MultipartFile file);

    UserOverviewVO getOverview();

    UserStatsVO getStats();
}
