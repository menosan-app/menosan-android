package app.menosan.android.feature.photo

import app.menosan.android.core.model.EntryDraft
import app.menosan.android.core.model.EntrySource
import app.menosan.android.core.model.Taxonomy
import app.menosan.android.data.remote.dto.PhotoSuggestionDto
import app.menosan.android.feature.logging.EntryField
import app.menosan.android.feature.logging.EntryFormState

data class PhotoReview(
    val form: EntryFormState,
    val aiSuggested: Set<EntryField>,
    val warning: String,
    val confirmed: Boolean = false,
) {
    fun edit(next: EntryFormState): PhotoReview = copy(form = next, aiSuggested = aiSuggested - changedFields(form, next))

    fun confirmField(field: EntryField): PhotoReview = copy(aiSuggested = aiSuggested - field)

    fun withConfirmed(value: Boolean): PhotoReview = copy(confirmed = value)

    fun toDraft(taxonomy: Taxonomy): EntryDraft? = form.toDraft(taxonomy, EntrySource.PHOTO)

    companion object {
        fun from(suggestion: PhotoSuggestionDto, warning: String, taxonomy: Taxonomy): PhotoReview {
            val draft = EntryDraft(suggestion.name, suggestion.subcategory, suggestion.quantity, EntrySource.PHOTO)
            val form = EntryFormState.from(draft, taxonomy).let {
                if (it.category == null) it.copy(category = suggestion.category) else it
            }.withName(suggestion.name)
            return PhotoReview(form = form, aiSuggested = EntryField.entries.toSet(), warning = warning)
        }

        fun changedFields(old: EntryFormState, new: EntryFormState): Set<EntryField> = buildSet {
            if (old.category != new.category) add(EntryField.CATEGORY)
            if (old.subcategory != new.subcategory) add(EntryField.SUBCATEGORY)
            if (old.name != new.name) add(EntryField.NAME)
            if (old.quantity != new.quantity) add(EntryField.QUANTITY)
        }
    }
}
