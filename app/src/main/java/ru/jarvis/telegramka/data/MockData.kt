package ru.jarvis.telegramka.data

object MockData {

    val currentUser = User("current-user", "Lex", "lex@example.com", "@lex")

    // Let's create a list of all users in the system for finding by nickname
    val allUsers = listOf(
        currentUser,
        User("1", "Alice", "alice@example.com", "@alice"),
        User("2", "Bob", "bob@example.com", "@bob"),
        User("3", "Charlie", "charlie@example.com", "@charlie"),
        User("4", "David", "david@example.com", "@david")
    )


    val chats = listOf(
        Chat(
            id = "1",
            name = "Alice",
            nickname = "@alice",
            lastMessage = "Hey, how are you?",
            lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 5,
            unread = 2,
            avatarUrl = "https://i.pravatar.cc/150?u=alice"
        ),
        Chat(
            id = "2",
            name = "Bob",
            nickname = "@bob",
            lastMessage = "See you tomorrow!",
            lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 60 * 2,
            avatarUrl = "https://i.pravatar.cc/150?u=bob"
        ),
        Chat(
            id = "3",
            name = "Charlie",
            nickname = "@charlie",
            lastMessage = "Thanks!",
            lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 60 * 24,
            avatarUrl = "https://i.pravatar.cc/150?u=charlie"
        )
    )

    val messages = listOf(
        Message("1", "1", "current-user", "Hi Alice!", System.currentTimeMillis() - 1000 * 60 * 10),
        Message("2", "1", "1", "Hi Lex! I'm good, thanks. How about you?", System.currentTimeMillis() - 1000 * 60 * 9),
        Message("3", "1", "current-user", "Doing great! Just working on this messenger app.", System.currentTimeMillis() - 1000 * 60 * 8),
        Message("4", "1", "1", "Wow, sounds cool!", System.currentTimeMillis() - 1000 * 60 * 7),
        Message("5", "1", "1", "Hey, how are you?", System.currentTimeMillis() - 1000 * 60 * 5),
        Message("6", "2", "2", "See you tomorrow!", System.currentTimeMillis() - 1000 * 60 * 60 * 2),
        Message("7", "3", "3", "Thanks!", System.currentTimeMillis() - 1000 * 60 * 60 * 24),
    )
}
