package com.server.server.dto.importing;

import java.util.List;

public record ImportResult(int importedCount, int failedCount, List<String> errors) {}
