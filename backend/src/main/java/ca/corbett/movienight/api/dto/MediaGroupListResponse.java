package ca.corbett.movienight.api.dto;

import java.util.List;

/**
 * Paginated list response envelope for MediaGroup collections.
 * <p>
 * All list/search endpoints wrap their results in this envelope per the API contract.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class MediaGroupListResponse {

    private final List<MediaGroupResponse> items;
    private final long totalCount;
    private final int pageNumber;
    private final int pageSize;

    public MediaGroupListResponse(List<MediaGroupResponse> items, long totalCount, int pageNumber, int pageSize) {
        this.items = items;
        this.totalCount = totalCount;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    public List<MediaGroupResponse> getItems() {
        return items;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }
}
