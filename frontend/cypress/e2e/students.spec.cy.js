// UC-STU-deactivate-student, extension 5a: cancelling at the confirmation sends no request
describe('Deactivate a student', () => {
  beforeEach(() => {
    cy.login('b.wei@abc.edu', '123456')
  })

  it('5a: cancelling at the confirmation leaves the student enabled and sends no request', () => {
    cy.intercept('PUT', '**/students/*/deactivate', cy.spy().as('deactivateRequest'))

    cy.visit('/students')
    cy.contains('.el-table__row', 'j.smith@abc.edu').as('johnRow')
    cy.get('@johnRow').contains('Enabled').should('be.visible')

    cy.get('@johnRow').find('.deactivate-student-button').click()

    // Step 4: the dialog states the consequences before asking for confirmation
    cy.get('.el-message-box').within(() => {
      cy.contains('Deactivate John Smith?').should('be.visible')
      cy.contains('This is not a delete').should('be.visible')
      cy.contains('button', 'Cancel').click()
    })

    cy.get('.el-message-box').should('not.exist')
    cy.get('@deactivateRequest').should('not.have.been.called')
    cy.url().should('include', '/students') // The row click did not navigate to the dashboard
    cy.get('@johnRow').contains('Enabled').should('be.visible')
  })

  it('an unexpected server error is reported, does not escape uncaught, and the list is reloaded', () => {
    cy.intercept('PUT', '**/students/*/deactivate', {
      statusCode: 500,
      body: { flag: false, code: 500, message: 'A server internal error occurs.', data: null }
    }).as('deactivate')
    cy.intercept('POST', '**/students/search*').as('search')

    cy.visit('/students')
    cy.wait('@search')
    cy.contains('.el-table__row', 'j.smith@abc.edu').find('.deactivate-student-button').click()
    cy.get('.el-message-box').contains('button', 'Deactivate').click()

    cy.wait('@deactivate')
    cy.contains('A server internal error occurs.').should('be.visible') // the global interceptor's toast
    cy.wait('@search') // reloaded after the failure
    cy.url().should('include', '/students')
  })
})
