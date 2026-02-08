package com.yourcompany.tradesketch.domain.usecase

import com.yourcompany.tradesketch.domain.model.Project
import com.yourcompany.tradesketch.domain.model.ProjectTemplate
import javax.inject.Inject

class CreateProjectFromTemplateUseCase @Inject constructor() {
    operator fun invoke(template: ProjectTemplate, customName: String? = null): Project {
        return template.createProject(customName ?: template.displayName())
    }
}
