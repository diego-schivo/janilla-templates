package com.janilla.templates.website;

import com.janilla.cms.Types;

public record Meta(String title, String description, @Types(Media.class) Long image) {
}