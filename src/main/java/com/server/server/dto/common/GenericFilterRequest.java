package com.server.server.dto.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class GenericFilterRequest {
    private Integer page;
    private Integer size;
    private String search;
    private String sortBy;
    private String sortDir;
}
