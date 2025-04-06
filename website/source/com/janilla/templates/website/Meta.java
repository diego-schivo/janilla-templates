package com.janilla.templates.website;

public record Meta(String title, @Types(Media.class) Long image, String description) {
}