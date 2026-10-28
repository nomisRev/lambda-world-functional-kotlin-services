
---
magic-move
---



```kotlin
sealed interface Result
data class Success(val user: User) : Result
data object AlreadyExists : Result

when(val result = insertUser(body)) {
  is Success -> respond(result.user)
  AlreadyExists -> fail(UserAlreadyExists)
}
```

---