package com.kunzisoft.keepass.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kunzisoft.keepass.app.database.IOActionTask
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.model.*
import com.kunzisoft.keepass.otp.OtpElement


class EntryEditViewModel: NodeEditViewModel() {

    private var mTemplate: Template? = null
    private val mTempAttachments = mutableListOf<EntryAttachmentState>()

    val templatesEntry : LiveData<TemplatesEntry> get() = _templatesEntry
    private val _templatesEntry = MutableLiveData<TemplatesEntry>()

    val requestEntryInfoUpdate : LiveData<EntryUpdate> get() = _requestEntryInfoUpdate
    private val _requestEntryInfoUpdate = SingleLiveEvent<EntryUpdate>()
    val onEntrySaved : LiveData<EntrySave> get() = _onEntrySaved
    private val _onEntrySaved = SingleLiveEvent<EntrySave>()

    val onTemplateChanged : LiveData<Template> get() = _onTemplateChanged
    private val _onTemplateChanged = SingleLiveEvent<Template>()

    val requestPasswordSelection : LiveData<Field> get() = _requestPasswordSelection
    private val _requestPasswordSelection = SingleLiveEvent<Field>()
    val onPasswordSelected : LiveData<Field> get() = _onPasswordSelected
    private val _onPasswordSelected = SingleLiveEvent<Field>()

    val requestCustomFieldEdition : LiveData<Field> get() = _requestCustomFieldEdition
    private val _requestCustomFieldEdition = SingleLiveEvent<Field>()
    val onCustomFieldEdited : LiveData<FieldEdition> get() = _onCustomFieldEdited
    private val _onCustomFieldEdited = SingleLiveEvent<FieldEdition>()
    val onCustomFieldError : LiveData<Void?> get() = _onCustomFieldError
    private val _onCustomFieldError = SingleLiveEvent<Void?>()

    val requestSetupOtp : LiveData<Void?> get() = _requestSetupOtp
    private val _requestSetupOtp = SingleLiveEvent<Void?>()
    val onOtpCreated : LiveData<OtpElement> get() = _onOtpCreated
    private val _onOtpCreated = SingleLiveEvent<OtpElement>()

    val onBuildNewAttachment : LiveData<AttachmentBuild> get() = _onBuildNewAttachment
    private val _onBuildNewAttachment = SingleLiveEvent<AttachmentBuild>()
    val onStartUploadAttachment : LiveData<AttachmentUpload> get() = _onStartUploadAttachment
    private val _onStartUploadAttachment = SingleLiveEvent<AttachmentUpload>()
    val attachmentDeleted : LiveData<Attachment> get() = _attachmentDeleted
    private val _attachmentDeleted = SingleLiveEvent<Attachment>()
    val onAttachmentAction : LiveData<EntryAttachmentState?> get() = _onAttachmentAction
    private val _onAttachmentAction = MutableLiveData<EntryAttachmentState?>()
    val onBinaryPreviewLoaded : LiveData<AttachmentPosition> get() = _onBinaryPreviewLoaded
    private val _onBinaryPreviewLoaded = SingleLiveEvent<AttachmentPosition>()


    fun loadTemplateEntry(database: Database,
                          entry: Entry?,
                          isTemplate: Boolean,
                          registerInfo: RegisterInfo?,
                          searchInfo: SearchInfo?) {
        IOActionTask(
            {
                val templates = database.getTemplates(isTemplate)
                val entryTemplate = mTemplate
                    ?: (entry?.let { database.getTemplate(it) }
                        ?: Template.STANDARD)
                var entryInfo: EntryInfo? = null
                // Decode the entry / load entry info
                entry?.let {
                    database.decodeEntryWithTemplateConfiguration(it).let { entry ->
                        // Load entry info
                        entry.getEntryInfo(database, true).let { tempEntryInfo ->
                            // Retrieve data from registration
                            (registerInfo?.searchInfo ?: searchInfo)?.let { tempSearchInfo ->
                                tempEntryInfo.saveSearchInfo(database, tempSearchInfo)
                            }
                            registerInfo?.let { regInfo ->
                                tempEntryInfo.saveRegisterInfo(database, regInfo)
                            }
                            entryInfo = tempEntryInfo
                        }
                    }
                }
                TemplatesEntry(templates, entryTemplate, entryInfo)
            },
            { templatesEntry ->
                _templatesEntry.value = templatesEntry
            }
        ).execute()
    }

    fun changeTemplate(template: Template) {
        this.mTemplate = template
        if (_onTemplateChanged.value != template) {
            _onTemplateChanged.value = template
        }
    }

    fun requestEntryInfoUpdate(database: Database?, entry: Entry?, parent: Group?) {
        _requestEntryInfoUpdate.value = EntryUpdate(database, entry, parent)
    }

    fun saveEntryInfo(database: Database?, entry: Entry?, parent: Group?, entryInfo: EntryInfo) {
        IOActionTask(
            {
                removeTempAttachmentsNotCompleted(entryInfo)
                entry?.let { oldEntry ->
                    // Create a clone
                    var newEntry = Entry(oldEntry)

                    // Build info
                    newEntry.setEntryInfo(database, entryInfo)

                    // Encode entry properties for template
                    _onTemplateChanged.value?.let { template ->
                        newEntry =
                            database?.encodeEntryWithTemplateConfiguration(newEntry, template)
                                ?: newEntry
                    }

                    // Delete temp attachment if not used
                    mTempAttachments.forEach { tempAttachmentState ->
                        val tempAttachment = tempAttachmentState.attachment
                        database?.attachmentPool?.let { binaryPool ->
                            if (!newEntry.getAttachments(binaryPool).contains(tempAttachment)) {
                                database.removeAttachmentIfNotUsed(tempAttachment)
                            }
                        }
                    }

                    // Return entry to save
                    EntrySave(oldEntry, newEntry, parent)
                }
            },
            { entrySave ->
                entrySave?.let {
                    _onEntrySaved.value = it
                }
            }
        ).execute()
    }

    private fun removeTempAttachmentsNotCompleted(entryInfo: EntryInfo) {
        // Do not save entry in upload progression
        mTempAttachments.forEach { attachmentState ->
            if (attachmentState.streamDirection == StreamDirection.UPLOAD) {
                when (attachmentState.downloadState) {
                    AttachmentState.START,
                    AttachmentState.IN_PROGRESS,
                    AttachmentState.CANCELED,
                    AttachmentState.ERROR -> {
                        // Remove attachment not finished from info
                        entryInfo.attachments = entryInfo.attachments.toMutableList().apply {
                            remove(attachmentState.attachment)
                        }
                    }
                    else -> {
                    }
                }
            }
        }
    }

    fun requestPasswordSelection(passwordField: Field) {
        _requestPasswordSelection.value = passwordField
    }

    fun selectPassword(passwordField: Field) {
        _onPasswordSelected.value = passwordField
    }

    fun requestCustomFieldEdition(customField: Field) {
        _requestCustomFieldEdition.value = customField
    }

    fun addCustomField(newField: Field) {
        _onCustomFieldEdited.value = FieldEdition(null, newField)
    }

    fun editCustomField(oldField: Field, newField: Field) {
        _onCustomFieldEdited.value = FieldEdition(oldField, newField)
    }

    fun removeCustomField(oldField: Field) {
        _onCustomFieldEdited.value = FieldEdition(oldField, null)
    }

    fun showCustomFieldEditionError() {
        _onCustomFieldError.call()
    }

    fun setupOtp() {
        _requestSetupOtp.call()
    }

    fun createOtp(otpElement: OtpElement) {
        _onOtpCreated.value = otpElement
    }

    fun buildNewAttachment(attachmentToUploadUri: Uri, fileName: String) {
        _onBuildNewAttachment.value = AttachmentBuild(attachmentToUploadUri, fileName)
    }

    fun startUploadAttachment(attachmentToUploadUri: Uri, attachment: Attachment) {
        _onStartUploadAttachment.value = AttachmentUpload(attachmentToUploadUri, attachment)
    }

    fun deleteAttachment(attachment: Attachment) {
        _attachmentDeleted.value = attachment
    }

    fun onAttachmentAction(entryAttachmentState: EntryAttachmentState?) {
        // Add in temp list
        if (mTempAttachments.contains(entryAttachmentState)) {
            mTempAttachments.remove(entryAttachmentState)
        }
        if (entryAttachmentState != null) {
            mTempAttachments.add(entryAttachmentState)
        }
        _onAttachmentAction.value = entryAttachmentState
    }

    fun binaryPreviewLoaded(entryAttachmentState: EntryAttachmentState, viewPosition: Float) {
        _onBinaryPreviewLoaded.value = AttachmentPosition(entryAttachmentState, viewPosition)
    }

    data class TemplatesEntry(val templates: List<Template>, val defaultTemplate: Template, val entryInfo: EntryInfo?)
    data class EntryUpdate(val database: Database?, val entry: Entry?, val parent: Group?)
    data class EntrySave(val oldEntry: Entry, val newEntry: Entry, val parent: Group?)
    data class FieldEdition(val oldField: Field?, val newField: Field?)
    data class AttachmentBuild(val attachmentToUploadUri: Uri, val fileName: String)
    data class AttachmentUpload(val attachmentToUploadUri: Uri, val attachment: Attachment)
    data class AttachmentPosition(val entryAttachmentState: EntryAttachmentState, val viewPosition: Float)

    companion object {
        private val TAG = EntryEditViewModel::class.java.name
    }
}