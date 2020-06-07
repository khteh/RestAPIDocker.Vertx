package com.restapi.vertx.models;

public class Book {
    private Long id;
	private int page_count;
	private String title;
	private String isbn;
	private Author author;
	public Book() {}
    public Book(String title, String isbn, int count, Author author) { this.title = title; this.isbn = isbn; page_count = count; this.author = author;}
    public Book(Long id, String title, String isbn, int count, Author author) { this.id = id; this.title = title; this.isbn = isbn; page_count = count; this.author = author;}
    public Long getId() { return id; }
    public String getIsbn() { return isbn; }
    public int getPageCount() { return page_count; }    
    public Author getAuthor() { return author; }
    public String getTitle() { return title; }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }  
    public void setAuthor(Author author) { this.author = author; }
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
		return "Book { id = " + id + ", title = '" + title + "', isbn = '" + isbn + "', pageCount = " + page_count + ", author = " + author + "}";
	}    
}