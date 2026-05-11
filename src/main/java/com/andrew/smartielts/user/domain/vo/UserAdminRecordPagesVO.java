package com.andrew.smartielts.user.domain.vo;

import com.andrew.smartielts.common.page.PageResult;
import com.andrew.smartielts.record.domain.vo.UserRecordItemVO;
import lombok.Data;

@Data
public class UserAdminRecordPagesVO {

    private PageResult<UserRecordItemVO> activeRecords;

    private PageResult<UserRecordItemVO> deletedRecords;
}
