package com.quasar.art.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphDTO {
    
    private List<Node> nodes;
    private List<Edge> edges;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Node {
        private Long id;
        private String name;
        private String title;
        private String summary;
        private String author;
        private Integer publishYear;
        private Integer parseStatus;
        private Integer category;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Edge {
        private String source;
        private String target;
        private double weight;
        private String relationType;
    }
}