/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.docs.guestbook.service.impl;

import java.util.Date;
import java.util.List;

import com.liferay.docs.guestbook.model.GuestbookEntry;
import com.liferay.docs.guestbook.service.GuestbookEntryLocalService;
import com.liferay.portal.kernel.exception.SystemException;
import org.osgi.service.component.annotations.Component;

import com.liferay.docs.guestbook.exception.GuestbookNameException;
import com.liferay.docs.guestbook.model.Guestbook;
import com.liferay.docs.guestbook.service.base.GuestbookLocalServiceBaseImpl;
import com.liferay.portal.aop.AopService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.Validator;
import org.osgi.service.component.annotations.Reference;

/**
 * The implementation of the guestbook local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the <code>com.liferay.docs.guestbook.service.GuestbookLocalService</code> interface.
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author liferay
 * @see GuestbookLocalServiceBaseImpl
 */
@Component(
	property = "model.class.name=com.liferay.docs.guestbook.model.Guestbook",
	service = AopService.class
)
public class GuestbookLocalServiceImpl extends GuestbookLocalServiceBaseImpl {

	//This method adds a guestbook to the database.
	// It retrieves metadata from the environment,along with data passed from the user
	public Guestbook addGuestbook(long userId, String name,
								  ServiceContext serviceContext) throws PortalException, PortalException {

		long groupId = serviceContext.getScopeGroupId();

		User user = userLocalService.getUserById(userId);

		Date now = new Date();

		validate(name);

		long guestbookId = counterLocalService.increment();

		Guestbook guestbook = guestbookPersistence.create(guestbookId);

		guestbook.setUuid(serviceContext.getUuid());
		guestbook.setUserId(userId);
		guestbook.setGroupId(groupId);
		guestbook.setCompanyId(user.getCompanyId());
		guestbook.setUserName(user.getFullName());
		guestbook.setCreateDate(serviceContext.getCreateDate(now));
		guestbook.setModifiedDate(serviceContext.getModifiedDate(now));
		guestbook.setName(name);
		guestbook.setExpandoBridgeAttributes(serviceContext);

		guestbookPersistence.update(guestbook);

		return guestbook;
	}

	// These methods call the finders you generated with Service Builder.
	public List<Guestbook> getGuestbooks(long groupId) {

		return guestbookPersistence.findByGroupId(groupId);
	}

	public List<Guestbook> getGuestbooks(long groupId, int start, int end,
										 OrderByComparator<Guestbook> obc) {

		return guestbookPersistence.findByGroupId(groupId, start, end, obc);
	}

	public List<Guestbook> getGuestbooks(long groupId, int start, int end) {

		return guestbookPersistence.findByGroupId(groupId, start, end);
	}

	public int getGuestbooksCount(long groupId) {

		return guestbookPersistence.countByGroupId(groupId);
	}


	//This method uses Liferay DXP’s Validator to make sure the user entered text for the guestbook name
	protected void validate(String name) throws PortalException {
		if (Validator.isNull(name)) {
			throw new GuestbookNameException();
		}
	}

	public Guestbook updateGuestbook(long userId, long guestbookId,
									 String name, ServiceContext serviceContext) throws PortalException,
			SystemException {

		Date now = new Date();

		validate(name);

		Guestbook guestbook = getGuestbook(guestbookId);

		User user = userLocalService.getUser(userId);

		guestbook.setUserId(userId);
		guestbook.setUserName(user.getFullName());
		guestbook.setModifiedDate(serviceContext.getModifiedDate(now));
		guestbook.setName(name);
		guestbook.setExpandoBridgeAttributes(serviceContext);

		guestbookPersistence.update(guestbook);

		return guestbook;
	}


	public Guestbook deleteGuestbook(long guestbookId,
									 ServiceContext serviceContext) throws PortalException,
			SystemException {

		Guestbook guestbook = getGuestbook(guestbookId);

		List<GuestbookEntry> entries = _guestbookEntryLocalService.getGuestbookEntries(
				serviceContext.getScopeGroupId(), guestbookId);

		for (GuestbookEntry entry : entries) {
			_guestbookEntryLocalService.deleteGuestbookEntry(entry.getEntryId());
		}

		guestbook = deleteGuestbook(guestbook);

		return guestbook;
	}

	@Reference
	private GuestbookEntryLocalService _guestbookEntryLocalService;
}