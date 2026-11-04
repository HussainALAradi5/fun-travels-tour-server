package com.server.server.dto.common;

import java.util.List;

public record ImportResult(int importedCount, int failedCount, List<String> errors) {}
