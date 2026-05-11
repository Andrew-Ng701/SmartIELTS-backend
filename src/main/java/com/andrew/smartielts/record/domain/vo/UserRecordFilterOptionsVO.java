package com.andrew.smartielts.record.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserRecordFilterOptionsVO {

    private List<String> moduleTypes;

    private List<String> recordStates;
}
