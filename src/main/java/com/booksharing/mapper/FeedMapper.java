package com.booksharing.mapper;

import com.booksharing.dto.res.CommentResponse;
import com.booksharing.dto.res.PostResponse;
import com.booksharing.entity.Comment;
import com.booksharing.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FeedMapper {

    @Mapping(target = "authorId", source = "post.author.id")
    @Mapping(target = "authorName", source = "post.author.name")
    @Mapping(target = "authorAvatarUrl", source = "post.author.avatarUrl")
    PostResponse toResponse(Post post, long commentsCount);

    @Mapping(target = "postId", source = "post.id")
    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "authorName", source = "author.name")
    @Mapping(target = "authorAvatarUrl", source = "author.avatarUrl")
    CommentResponse toResponse(Comment comment);
}