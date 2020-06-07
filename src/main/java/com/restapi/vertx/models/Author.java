package com.restapi.vertx.models;

public class Author {
	private Long id;
	private String first_name;
	private String last_name;
	private String email;
	private String phone;
	public Author() {}
	public Author(Long id) { this.id = id; }
	public Author(String first, String last) { first_name = first; last_name = last; }
	public Author(Long id, String first, String last) { this.id = id; first_name = first; last_name = last; }
	public Long getId() { return id; }
	public String getFirstName() { return first_name; }
	public String getLastName() { return last_name; }
	public String getEmail() { return email; }
	public String getPhone() { return phone; }
	public void setId(Long id) { this.id = id; }
	public void setFirstName(String name) { first_name = name; }
	public void setLastName(String name) { last_name = name; }
	public void setEmail(String email) { this.email = email; }
	public void setPhone(String phone) { this.phone = phone; }
	@Override
	public boolean equals(Object o) {
		return this == o || (o != null && getClass() == o.getClass() && id.equals(((Author)o).id));
	}
	@Override
	public int hashCode() { return id.hashCode(); }
	@Override
	public String toString() {
		return "Author { id = " + id + ", first_name = '" + first_name + "', last_name = '" + last_name + "'}";
	}
}