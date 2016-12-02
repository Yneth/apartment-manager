package ua.abond.lab4.config.core.web.support;

public class DefaultPageable implements Pageable {
    private int count;
    private int pageSize;
    private int pageNumber;
    private String sortBy;
    private SortOrder sortOrder;

    public DefaultPageable(int pageNumber, int pageSize, String sortBy, SortOrder sortOrder) {
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
    }

    @Override
    public int getOffset() {
        return pageSize * (pageNumber - 1);
    }

    @Override
    public int getPageNumber() {
        return pageNumber;
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    @Override
    public String sortBy() {
        return sortBy;
    }

    @Override
    public boolean hasPrevious() {
        return (pageNumber - 1) > 0;
    }

    @Override
    public Pageable first() {
        return new DefaultPageable(1, pageSize, sortBy, sortOrder);
    }

    @Override
    public Pageable next() {
        return new DefaultPageable(pageNumber + 1, pageSize, sortBy, sortOrder);
    }

    @Override
    public Pageable previousOrFirst() {
        if (!hasPrevious()) {
            return first();
        }
        return new DefaultPageable(pageNumber + 1, pageSize, sortBy, sortOrder);
    }
}