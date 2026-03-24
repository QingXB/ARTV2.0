package com.quasar.art.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
public class PythonOutlineDTO {
    private List<PaperInfo> papers;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaperInfo {
        private String title;
        // 保证发给 Python 的 JSON 字段名是带下划线的，和 Python 对应
        @JsonProperty("research_question") 
        private String researchQuestion;
        private String methodology;
        private String conclusion;
    }
}