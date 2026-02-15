package com.reelsplit.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.reelsplit.data.local.dao.VideoDao
import com.reelsplit.data.local.mapper.toDomain
import com.reelsplit.domain.model.Video
import timber.log.Timber

/**
 * Paging source for loading video history with offset-based pagination.
 *
 * Delegates to [VideoDao.getVideosPage] for efficient database queries.
 * Videos are ordered by creation date (newest first), consistent with the
 * DAO's query ordering.
 *
 * ### Invalidation
 * This source must be recreated when the underlying data changes. The [Pager]
 * in [HistoryViewModel] uses a factory lambda to handle this automatically.
 * Call [invalidate] to trigger a refresh (e.g., after deleting a video).
 *
 * @param videoDao The DAO for querying the video database.
 */
class VideoHistoryPagingSource(
    private val videoDao: VideoDao
) : PagingSource<Int, Video>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Video> {
        val page = params.key ?: INITIAL_PAGE

        return try {
            val offset = page * PAGE_SIZE
            val entities = videoDao.getVideosPage(limit = PAGE_SIZE, offset = offset)
            val videos = entities.map { it.toDomain() }

            // Boundary detection using result size — avoids an extra COUNT(*) query
            // and the race condition between getVideosPage and getVideoCount.
            val nextKey = if (videos.size < PAGE_SIZE) null else page + 1
            val prevKey = if (page == INITIAL_PAGE) null else page - 1

            LoadResult.Page(
                data = videos,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to load video history page $page")
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Video>): Int? {
        // Find the closest page to the anchor position
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    companion object {
        /** Starting page index (0-based). */
        private const val INITIAL_PAGE = 0

        /** Default page size for history queries. */
        const val PAGE_SIZE = 20
    }
}
