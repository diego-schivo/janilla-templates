package com.janilla.templates.website;

public record Meta(String title, String description, @Types(Media.class) Long image) {
}