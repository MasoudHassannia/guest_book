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

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetLinkConstants;
import com.liferay.docs.guestbook.exception.GuestbookNameException;
import com.liferay.docs.guestbook.model.Guestbook;
import com.liferay.docs.guestbook.model.GuestbookEntry;
import com.liferay.docs.guestbook.service.GuestbookEntryLocalService;
import com.liferay.docs.guestbook.service.base.GuestbookLocalServiceBaseImpl;
import com.liferay.portal.aop.AopService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.search.Indexable;
import com.liferay.portal.kernel.search.IndexableType;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.workflow.WorkflowHandlerRegistryUtil;

import java.util.Date;
import java.util.List;

import org.osgi.service.component.annotations.Component;
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

	@Indexable(type = IndexableType.REINDEX)
	public Guestbook addGuestbook(
			long userId, String name, ServiceContext serviceContext)
		throws PortalException, PortalException {

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

		//		This 4 setter manually populates the status fields and sets the workflow status as
		//		a draft in the GB_GuestbookEntry database table.
		guestbook.setStatus(WorkflowConstants.STATUS_DRAFT);
		guestbook.setStatusByUserId(userId);
		guestbook.setStatusByUserName(user.getFullName());
		guestbook.setStatusDate(serviceContext.getModifiedDate(null));

		guestbookPersistence.update(guestbook);

		resourceLocalService.addResources(
			user.getCompanyId(), groupId, userId, Guestbook.class.getName(),
			guestbookId, false, true, true);

		AssetEntry assetEntry = assetEntryLocalService.updateEntry(
			userId, groupId, guestbook.getCreateDate(),
			guestbook.getModifiedDate(), Guestbook.class.getName(), guestbookId,
			guestbook.getUuid(), 0, serviceContext.getAssetCategoryIds(),
			serviceContext.getAssetTagNames(), true, true, null, null, null,
			null, ContentTypes.TEXT_HTML, guestbook.getName(), null, null, null,
			null, 0, 0, null);

		assetLinkLocalService.updateLinks(
			userId, assetEntry.getEntryId(),
			serviceContext.getAssetLinkEntryIds(),
			AssetLinkConstants.TYPE_RELATED);

		//		The call to startWorkflowInstance detects whether workflow is installed and enabled.
		//		If it isn't, the added entity is automatically marked as approved.
		WorkflowHandlerRegistryUtil.startWorkflowInstance(
			guestbook.getCompanyId(), guestbook.getGroupId(),
			guestbook.getUserId(), Guestbook.class.getName(),
			guestbook.getPrimaryKey(), guestbook, serviceContext);

		return guestbook;
	}

	// These methods call the finders you generated with Service Builder.

	@Indexable(type = IndexableType.DELETE)
	public Guestbook deleteGuestbook(
			long guestbookId, ServiceContext serviceContext)
		throws PortalException {

		Guestbook guestbook = getGuestbook(guestbookId);

		List<GuestbookEntry> entries =
			_guestbookEntryLocalService.getGuestbookEntries(
				serviceContext.getScopeGroupId(), guestbookId);

		for (GuestbookEntry entry : entries) {
			_guestbookEntryLocalService.deleteGuestbookEntry(
				entry.getEntryId());
		}

		guestbook = deleteGuestbook(guestbook);

		resourceLocalService.deleteResource(
			serviceContext.getCompanyId(), Guestbook.class.getName(),
			ResourceConstants.SCOPE_INDIVIDUAL, guestbookId);

		AssetEntry assetEntry = assetEntryLocalService.fetchEntry(
			Guestbook.class.getName(), guestbookId);

		assetLinkLocalService.deleteLinks(assetEntry.getEntryId());

		assetEntryLocalService.deleteEntry(assetEntry);

		workflowInstanceLinkLocalService.deleteWorkflowInstanceLinks(
			guestbook.getCompanyId(), guestbook.getGroupId(),
			Guestbook.class.getName(), guestbook.getGuestbookId());

		return guestbook;
	}

	public List<Guestbook> getGuestbooks(long groupId) {
		return guestbookPersistence.findByGroupId(groupId);
	}

	public List<Guestbook> getGuestbooks(long groupId, int status) {
		return guestbookPersistence.findByG_S(
			groupId, WorkflowConstants.STATUS_APPROVED);
	}

	public List<Guestbook> getGuestbooks(long groupId, int start, int end) {
		return guestbookPersistence.findByGroupId(groupId, start, end);
	}

	public List<Guestbook> getGuestbooks(
		long groupId, int start, int end, OrderByComparator<Guestbook> obc) {

		return guestbookPersistence.findByGroupId(groupId, start, end, obc);
	}

	//This method uses Liferay DXP's Validator to make sure the user entered text for the guestbook name
	public int getGuestbooksCount(long groupId) {
		return guestbookPersistence.countByGroupId(groupId);
	}

	@Indexable(type = IndexableType.REINDEX)
	public Guestbook updateGuestbook(
			long userId, long guestbookId, String name,
			ServiceContext serviceContext)
		throws PortalException {

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

		resourceLocalService.updateResources(
			serviceContext.getCompanyId(), serviceContext.getScopeGroupId(),
			Guestbook.class.getName(), guestbookId,
			serviceContext.getModelPermissions());

		AssetEntry assetEntry = assetEntryLocalService.updateEntry(
			guestbook.getUserId(), guestbook.getGroupId(),
			guestbook.getCreateDate(), guestbook.getModifiedDate(),
			Guestbook.class.getName(), guestbookId, guestbook.getUuid(), 0,
			serviceContext.getAssetCategoryIds(),
			serviceContext.getAssetTagNames(), true, true,
			guestbook.getCreateDate(), null, null, null, ContentTypes.TEXT_HTML,
			guestbook.getName(), null, null, null, null, 0, 0,
			serviceContext.getAssetPriority());

		assetLinkLocalService.updateLinks(
			serviceContext.getUserId(), assetEntry.getEntryId(),
			serviceContext.getAssetLinkEntryIds(),
			AssetLinkConstants.TYPE_RELATED);

		return guestbook;
	}

	public Guestbook updateStatus(
			long userId, long guestbookId, int status,
			ServiceContext serviceContext)
		throws PortalException {

		User user = userLocalService.getUser(userId);
		Guestbook guestbook = getGuestbook(guestbookId);

		guestbook.setStatus(status);
		guestbook.setStatusByUserId(userId);
		guestbook.setStatusByUserName(user.getFullName());
		guestbook.setStatusDate(new Date());

		guestbookPersistence.update(guestbook);

		//		This if statement determines the visibility of the asset based on its workflow status.

		if (status == WorkflowConstants.STATUS_APPROVED) {
			assetEntryLocalService.updateVisible(
				Guestbook.class.getName(), guestbookId, true);
		}
		else {
			assetEntryLocalService.updateVisible(
				Guestbook.class.getName(), guestbookId, false);
		}

		return guestbook;
	}

	//	If this method is called, it's because your entity is returning from
	//	the workflow framework, and it's time to update the status values in the database
	protected void validate(String name) throws PortalException {
		if (Validator.isNull(name)) {
			throw new GuestbookNameException();
		}
	}

	@Reference
	private GuestbookEntryLocalService _guestbookEntryLocalService;

}