package com.example.storepromax.data.repository

import com.example.storepromax.data.local.dao.HistoryDao
import com.example.storepromax.data.local.entity.HistoryEntity
import com.example.storepromax.domain.model.Comment
import com.example.storepromax.domain.model.Post
import com.example.storepromax.domain.model.User
import com.example.storepromax.domain.repository.PostRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val historyDao: HistoryDao
) : PostRepository {

    override fun getPendingPosts(): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .whereEqualTo("status", "PENDING")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun updatePostStatus(postId: String, status: String, reason: String?): Result<Boolean> {
        return try {
            val updates = hashMapOf<String, Any>(
                "status" to status,
                "processedAt" to System.currentTimeMillis()
            )
            if (reason != null) {
                updates["rejectionReason"] = reason
            }

            firestore.collection("posts").document(postId).update(updates).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getApprovedPosts(): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .whereEqualTo("status", "APPROVED") // Chỉ lấy bài đã duyệt
            .orderBy("createdAt", Query.Direction.DESCENDING) // Bài mới nhất lên đầu
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val batch = db.batch()
            val postRef = db.collection("posts").document(postId)
            batch.delete(postRef)
            val commentsSnapshot = db.collection("comments")
                .whereEqualTo("postId", postId)
                .get()
                .await()
            for (doc in commentsSnapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun createPost(post: Post): Result<Boolean> {
        return try {
            val docRef = firestore.collection("posts").document()

            val finalPost = post.copy(id = docRef.id)

            docRef.set(finalPost).await()

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    override suspend fun toggleLike(postId: String, userId: String): Result<Boolean> {
        return try {
            val postRef = firestore.collection("posts").document(postId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val likedByUsers = snapshot.get("likedByUsers") as? List<String> ?: emptyList()

                if (likedByUsers.contains(userId)) {

                    transaction.update(postRef, "likedByUsers", FieldValue.arrayRemove(userId))
                    transaction.update(postRef, "likeCount", FieldValue.increment(-1))
                } else {

                    transaction.update(postRef, "likedByUsers", FieldValue.arrayUnion(userId))
                    transaction.update(postRef, "likeCount", FieldValue.increment(1))
                }
            }.await()

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    override fun getPostsByUser(userId: String): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getUserInfo(userId: String): Result<User> {
        return try {
            val snapshot = firestore.collection("users").document(userId).get().await()
            if (snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                if (user != null) Result.success(user) else Result.failure(Exception("User parsing error"))
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun addToViewHistory(post: Post) {
        val entity = HistoryEntity(
            id = post.id,
            userId = post.userId,
            userName = post.userName,
            userAvatar = post.userAvatar,
            title = post.title,
            content = post.content,
            price = post.price,
            images = post.images,
            condition = post.condition,
            grade = post.grade,
            likeCount = post.likeCount,
            commentCount = post.commentCount,
            status = post.status,
            createdAt = post.createdAt,
            viewedAt = System.currentTimeMillis()
        )
        historyDao.insert(entity)
    }

    override fun getViewHistory(): Flow<List<Post>> {
        return historyDao.getViewHistory().map { entities ->
            entities.map { entity ->
                Post(
                    id = entity.id,
                    userId = entity.userId,
                    userName = entity.userName,
                    userAvatar = entity.userAvatar,
                    title = entity.title,
                    content = entity.content,
                    price = entity.price,
                    images = entity.images,
                    condition = entity.condition,
                    grade = entity.grade,
                    likeCount = entity.likeCount,
                    commentCount = entity.commentCount,
                    status = entity.status,
                    createdAt = entity.createdAt,
                    rejectionReason = null,
                    searchKeywords = emptyList(),
                    processedAt = 0,
                    likedByUsers = emptyList()
                )
            }
        }
    }
    override suspend fun clearViewHistory() {
        historyDao.clearHistory()
    }
    override fun searchPosts(query: String): Flow<List<Post>> = callbackFlow {
        val keyword = query.lowercase().trim()

        val subscription = firestore.collection("posts")
            .whereEqualTo("status", "APPROVED")
            .whereArrayContains("searchKeywords", keyword)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val posts = snapshot.toObjects(Post::class.java).sortedByDescending { it.createdAt }
                    trySend(posts).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }
    override fun getCommentsForPost(postId: String): Flow<List<Comment>> = callbackFlow {
        val subscription = firestore.collection("comments")
            .whereEqualTo("postId", postId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val comments = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Comment::class.java)?.copy(id = doc.id)
                    }
                    trySend(comments).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun addComment(postId: String, comment: Comment): Result<Unit> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val commentRef = db.collection("comments").document()
            val postRef = db.collection("posts").document(postId)

            val finalComment = comment.copy(id = commentRef.id)

            db.runBatch { batch ->
                batch.set(commentRef, finalComment)

                batch.update(postRef, "commentCount", FieldValue.increment(1))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    override suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val batch = db.batch()
            val postRef = db.collection("posts").document(postId)
            val commentRef = db.collection("comments").document(commentId)
            batch.delete(commentRef)
            val repliesSnapshot = db.collection("comments")
                .whereEqualTo("parentId", commentId)
                .get()
                .await()
            for (doc in repliesSnapshot.documents) {
                batch.delete(doc.reference)
            }
            val totalDeleted = 1 + repliesSnapshot.documents.size
            batch.update(postRef, "commentCount", com.google.firebase.firestore.FieldValue.increment(-totalDeleted.toLong()))
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    override suspend fun updateComment(commentId: String, newContent: String): Result<Unit> {
        return try {
            FirebaseFirestore.getInstance().collection("comments")
                .document(commentId)
                .update("content", newContent).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override fun getProcessedPosts(): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .whereIn("status", listOf("APPROVED", "REJECTED"))
            .orderBy("processedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }
    override suspend fun updatePost(post: Post): Result<Boolean> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val finalPost = post.copy(
                status = "PENDING",
                rejectionReason = null,
                createdAt = System.currentTimeMillis()
            )
            db.collection("posts").document(finalPost.id).set(finalPost).await()
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    override suspend fun getPostById(postId: String): Result<Post> {
        return try {
            val snapshot = firestore.collection("posts").document(postId).get().await()
            if (snapshot.exists()) {
                val post = snapshot.toObject(Post::class.java)
                if (post != null) {
                    Result.success(post)
                } else {
                    Result.failure(Exception("Không thể parse dữ liệu bài viết"))
                }
            } else {
                Result.failure(Exception("Không tìm thấy bài viết"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}