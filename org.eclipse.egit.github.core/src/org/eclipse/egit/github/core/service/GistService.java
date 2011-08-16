/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.github.core.service;

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_COMMENTS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_FORK;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_GISTS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_PUBLIC;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_STAR;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_STARRED;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_USERS;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Assert;
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.client.PagedRequest;

/**
 * Service class for getting and list gists.
 */
public class GistService extends GitHubService {

	/**
	 * Create gist service
	 * 
	 * @param client
	 */
	public GistService(GitHubClient client) {
		super(client);
	}

	/**
	 * Check that gist id is non-null and non-empty
	 * 
	 * @param gistId
	 * @return gist id
	 */
	protected String checkGistId(String gistId) {
		if (gistId == null)
			throw new IllegalArgumentException("Gist id cannot be null"); //$NON-NLS-1$
		if (gistId.length() == 0)
			throw new IllegalArgumentException("Gist id cannot be empty"); //$NON-NLS-1$
		return gistId;
	}

	/**
	 * Get gist
	 * 
	 * @param id
	 * @return gist
	 * @throws IOException
	 */
	public Gist getGist(String id) throws IOException {
		checkGistId(id);
		StringBuilder uri = new StringBuilder(SEGMENT_GISTS);
		uri.append('/').append(id);
		GitHubRequest request = createRequest();
		request.setUri(uri);
		request.setType(Gist.class);
		return (Gist) client.get(request).getBody();
	}

	/**
	 * Get starred gists for currently authenticated user
	 * 
	 * @return list of gists
	 * @throws IOException
	 */
	public List<Gist> getStarredGists() throws IOException {
		PagedRequest<Gist> request = createPagedRequest();
		request.setUri(SEGMENT_GISTS + SEGMENT_STARRED);
		request.setType(new TypeToken<List<Gist>>() {
		}.getType());
		return getAll(request);
	}

	/**
	 * Create user gist paged request
	 * 
	 * @param user
	 * @param start
	 * @param size
	 * @return request
	 */
	protected PagedRequest<Gist> createUserGistRequest(String user, int start,
			int size) {
		Assert.notNull("User cannot be null", user); //$NON-NLS-1$
		StringBuilder uri = new StringBuilder(SEGMENT_USERS);
		uri.append('/').append(user);
		uri.append(SEGMENT_GISTS);
		PagedRequest<Gist> request = createPagedRequest(start, size);
		request.setUri(uri).setType(new TypeToken<List<Gist>>() {
		}.getType());
		return request;
	}

	/**
	 * Get gists for specified user
	 * 
	 * @param user
	 * @return list of gists
	 * @throws IOException
	 */
	public List<Gist> getGists(String user) throws IOException {
		PagedRequest<Gist> request = createUserGistRequest(user,
				PagedRequest.PAGE_FIRST, PagedRequest.PAGE_SIZE);
		return getAll(request);
	}

	/**
	 * Create page iterator for given user's gists
	 * 
	 * @param user
	 * @return gist page iterator
	 */
	public PageIterator<Gist> pageGists(final String user) {
		return pageGists(user, PagedRequest.PAGE_SIZE);
	}

	/**
	 * Create page iterator for given user's gists
	 * 
	 * @param user
	 * @param size
	 *            size of page
	 * @return gist page iterator
	 */
	public PageIterator<Gist> pageGists(final String user, final int size) {
		return pageGists(user, PagedRequest.PAGE_FIRST, size);
	}

	/**
	 * Create page iterator for given user's gists
	 * 
	 * @param user
	 * @param size
	 *            size of page
	 * @param start
	 *            starting page
	 * @return gist page iterator
	 */
	public PageIterator<Gist> pageGists(final String user, final int start,
			final int size) {
		PagedRequest<Gist> request = createUserGistRequest(user, start, size);
		return createPageIterator(request);
	}

	/**
	 * Create page iterator for all public gists
	 * 
	 * @return gist page iterator
	 */
	public PageIterator<Gist> pagePublicGists() {
		return pagePublicGists(PagedRequest.PAGE_SIZE);
	}

	/**
	 * Create page iterator for all public gists
	 * 
	 * @param size
	 *            size of page
	 * @return gist page iterator
	 */
	public PageIterator<Gist> pagePublicGists(final int size) {
		return pagePublicGists(PagedRequest.PAGE_FIRST, size);
	}

	/**
	 * Create page iterator for all public gists
	 * 
	 * @param start
	 *            starting page number
	 * @param size
	 *            size of page
	 * @return gist page iterator
	 */
	public PageIterator<Gist> pagePublicGists(final int start, final int size) {
		PagedRequest<Gist> request = createPagedRequest(start, size);
		request.setUri(SEGMENT_GISTS + SEGMENT_PUBLIC);
		request.setType(new TypeToken<List<Gist>>() {
		}.getType());
		return createPageIterator(request);
	}

	/**
	 * Create a gist
	 * 
	 * @param gist
	 * @return created gist
	 * @throws IOException
	 */
	public Gist createGist(Gist gist) throws IOException {
		Assert.notNull("Gist cannot be null", gist);
		return client.post(SEGMENT_GISTS, gist, Gist.class);
	}

	/**
	 * Update a gist
	 * 
	 * @param gist
	 * @return updated gist
	 * @throws IOException
	 */
	public Gist updateGist(Gist gist) throws IOException {
		Assert.notNull("Gist cannot be null", gist); //$NON-NLS-1$
		String id = gist.getId();
		Assert.notNull("Gist id cannot be null", id); //$NON-NLS-1$
		StringBuilder uri = new StringBuilder(SEGMENT_GISTS);
		uri.append('/').append(id);
		return client.post(uri.toString(), gist, Gist.class);
	}

	/**
	 * Create comment on specified gist id
	 * 
	 * @param gistId
	 * @param comment
	 * @return created issue
	 * @throws IOException
	 */
	public Comment createComment(String gistId, String comment)
			throws IOException {
		Assert.notNull("Gist id cannot be null", gistId); //$NON-NLS-1$
		Assert.notNull("Gist comment cannot be null", comment);
		StringBuilder uri = new StringBuilder(SEGMENT_GISTS);
		uri.append('/').append(gistId);
		uri.append(SEGMENT_COMMENTS);

		Map<String, String> params = Collections.singletonMap(
				IssueService.FIELD_BODY, comment);
		return client.post(uri.toString(), params, Comment.class);
	}

	/**
	 * Get comments for specified gist id
	 * 
	 * @param gistId
	 * @return list of comments
	 * @throws IOException
	 */
	public List<Comment> getComments(String gistId) throws IOException {
		Assert.notNull("Gist id cannot be null", gistId); //$NON-NLS-1$
		StringBuilder uri = new StringBuilder(SEGMENT_GISTS);
		uri.append('/').append(gistId);
		uri.append(SEGMENT_COMMENTS);
		PagedRequest<Comment> request = createPagedRequest();
		request.setUri(uri).setType(new TypeToken<List<Comment>>() {
		}.getType());
		return getAll(request);
	}

	/**
	 * Delete the Gist with the given id
	 * 
	 * @param gistId
	 * @throws IOException
	 */
	public void deleteGist(String gistId) throws IOException {
		Assert.notNull("Gist id cannot be null", gistId); //$NON-NLS-1$
		StringBuilder uri = new StringBuilder(SEGMENT_GISTS);
		uri.append('/').append(gistId);
		client.delete(uri.toString());
	}

	/**
	 * Delete the Gist comment with the given id
	 * 
	 * @param commentId
	 * @throws IOException
	 */
	public void deleteComment(String commentId) throws IOException {
		Assert.notNull("Gist comment id cannot be null", commentId); //$NON-NLS-1$
		StringBuilder uri = new StringBuilder(SEGMENT_GISTS + SEGMENT_COMMENTS);
		uri.append('/').append(commentId);
		client.delete(uri.toString());
	}

	/**
	 * Star the gist with the given id
	 * 
	 * @param gistId
	 * @throws IOException
	 */
	public void starGist(String gistId) throws IOException {
		checkGistId(gistId);
		Assert.notNull("Gist id cannot be null", gistId); //$NON-NLS-1$
		StringBuilder uri = new StringBuilder(SEGMENT_GISTS);
		uri.append('/').append(gistId);
		uri.append(SEGMENT_STAR);
		client.put(uri.toString(), null, null);
	}

	/**
	 * Unstar the gist with the given id
	 * 
	 * @param gistId
	 * @throws IOException
	 */
	public void unstarGist(String gistId) throws IOException {
		checkGistId(gistId);
		StringBuilder uri = new StringBuilder(SEGMENT_GISTS);
		uri.append('/').append(gistId);
		uri.append(SEGMENT_STAR);
		client.delete(uri.toString());
	}

	/**
	 * Check if a gist is starred
	 * 
	 * @param gistId
	 * @return true if starred, false if not starred
	 * @throws IOException
	 */
	public boolean isStarred(String gistId) throws IOException {
		checkGistId(gistId);
		StringBuilder uri = new StringBuilder(SEGMENT_GISTS);
		uri.append('/').append(gistId);
		uri.append(SEGMENT_STAR);
		return check(uri.toString());
	}

	/**
	 * Fork gist with given id
	 * 
	 * @param gistId
	 * @return forked gist
	 * @throws IOException
	 */
	public Gist forkGist(String gistId) throws IOException {
		checkGistId(gistId);
		StringBuilder uri = new StringBuilder(SEGMENT_GISTS);
		uri.append('/').append(gistId);
		uri.append(SEGMENT_FORK);
		return client.post(uri.toString(), null, Gist.class);
	}
}
