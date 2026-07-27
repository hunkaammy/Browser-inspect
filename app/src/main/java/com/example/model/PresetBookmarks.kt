package com.example.model

object PresetBookmarks {
    val list = listOf(
        Bookmark(
            title = "JSONPlaceholder Posts API",
            url = "https://jsonplaceholder.typicode.com/posts",
            category = "APIs & JSON",
            description = "Fake Online REST API for testing GET/POST/PUT JSON payloads"
        ),
        Bookmark(
            title = "ReqRes Users API",
            url = "https://reqres.in/api/users?page=1",
            category = "APIs & JSON",
            description = "Hosted REST API for testing paginated JSON user responses"
        ),
        Bookmark(
            title = "Dog CEO API",
            url = "https://dog.ceo/api/breeds/image/random/3",
            category = "APIs & JSON",
            description = "Free API returning JSON with dog image URLs"
        ),
        Bookmark(
            title = "Lorem Picsum Images Feed",
            url = "https://picsum.photos/v2/list?page=1&limit=8",
            category = "Images & Media",
            description = "Image gallery API returning dimensions, author info, and image URLs"
        ),
        Bookmark(
            title = "GitHub Linux Commits API",
            url = "https://api.github.com/repos/torvalds/linux/commits?per_page=5",
            category = "APIs & JSON",
            description = "Complex deeply-nested JSON with author, commit sha, and signatures"
        ),
        Bookmark(
            title = "Hacker News Top Stories",
            url = "https://hacker-news.firebaseio.com/v0/topstories.json",
            category = "APIs & JSON",
            description = "Official Firebase REST API returning array of top story IDs"
        ),
        Bookmark(
            title = "HTTPBin Request Tester",
            url = "https://httpbin.org/get",
            category = "Dev Tools",
            description = "Returns request headers, user agent, IP address, and query params as JSON"
        ),
        Bookmark(
            title = "Cat Facts Random API",
            url = "https://catfact.ninja/fact",
            category = "APIs & JSON",
            description = "Lightweight API for random cat facts in JSON"
        )
    )
}
