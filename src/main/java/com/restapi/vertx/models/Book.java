package com.restapi.vertx.models;

import io.vertx.core.json.JsonObject;

public class Book {
    private Long id;
	private int page_count;
	private String title;
	private String isbn;
	//private Author author;
	private Long author_id;
	public Book() {}
	public Book(JsonObject json) {
		id = json.getLong("id");
		page_count = json.getInteger("page_count");
		title = json.getString("title");
		isbn = json.getString("isbn");
		author_id = json.getLong("author_id");
	}		
    public Book(String title, String isbn, int count, Long author_id) { this.title = title; this.isbn = isbn; page_count = count; this.author_id = author_id;}
    public Book(Long id, String title, String isbn, int count, Long author_id) { this.id = id; this.title = title; this.isbn = isbn; page_count = count; this.author_id = author_id;}
    public Long getId() { return id; }
    public String getIsbn() { return isbn; }
    public int getPageCount() { return page_count; }    
    public Long getAuthorId() { return author_id; }
    public String getTitle() { return title; }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }  
    public void setAuthorId(Long author_id) { this.author_id = author_id; }
    public void setIsbnPublished(String isbn) { this.isbn = isbn; }
    public void setPageCount(int pageCount) { this.page_count = pageCount; }
	@Override
	public boolean equals(Object o) {
		return this == o || (o != null && getClass() == o.getClass() && id.equals(((Book)o).id));
	}
	@Override
	public int hashCode() { return id.hashCode(); }
	@Override
	public String toString() {
		return "Book { id = " + id + ", title = '" + title + "', isbn = '" + isbn + "', pageCount = " + page_count + ", author_id = " + author_id + "}";
	}
}