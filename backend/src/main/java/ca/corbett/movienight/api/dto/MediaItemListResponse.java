package ca.corbett.movienight.api.dto;

import java.util.List;

/**
 * Paginated list response envelope for MediaItem collections.
 * <p>
 * All list/search endpoints wrap their results in this envelope per the API contract.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public final class MediaItemListResponse {

    private final List<MediaItemResponse> items;
    private final long totalCount;
    private final int pageNumber;
    private final int pageSize;

    public MediaItemListResponse(List<MediaItemResponse> items, long totalCount, int pageNumber, int pageSize) {
        this.items = items;
        this.totalCount = totalCount;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    public List<MediaItemResponse> getItems() {
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
