// BR-evaluate-active-teammates on the Submit Peer Evaluations form. The teammate list and the evaluations already
// stored this week are stubbed, so each case controls who is active and who has been evaluated.
const ratings = (score) =>
  [1, 2, 3, 4, 5, 6].map((criterionId) => ({ criterionId, actualScore: score }))

const teammate = (id, firstName, lastName, enabled) => ({
  id,
  username: `${firstName.toLowerCase()}@abc.edu`,
  firstName,
  lastName,
  email: `${firstName.toLowerCase()}@abc.edu`,
  enabled
})

const stored = (evaluationId, evaluateeId, evaluateeName) => ({
  evaluationId,
  week: 'stubbed',
  evaluatorId: 4,
  evaluateeId,
  evaluateeName,
  ratings: ratings(8),
  totalScore: 48,
  publicComment: 'stored',
  privateComment: ''
})

const ok = (data) => ({ flag: true, code: 200, message: 'ok', data })

describe('Submit peer evaluations with deactivated or reactivated teammates', () => {
  beforeEach(() => {
    cy.login('j.smith@abc.edu', '123456')
    cy.intercept('PUT', '**/evaluations/*', (req) => req.reply(ok(req.body))).as('update')
    cy.intercept('POST', '**/evaluations', (req) =>
      req.reply(ok({ ...req.body, evaluationId: 900 + req.body.evaluateeId }))
    ).as('create')
  })

  it('offers a row to a teammate reactivated after the others were evaluated, and creates it on Update', () => {
    cy.intercept(
      'GET',
      '**/students/teams/*',
      ok([
        teammate(4, 'John', 'Smith', true),
        teammate(5, 'Eric', 'Hudson', true),
        teammate(6, 'Jerry', 'Moon', true) // reactivated: no stored evaluation this week
      ])
    )
    cy.intercept(
      'GET',
      '**/evaluations/evaluators/*/week/*',
      ok([stored(101, 4, 'John Smith'), stored(102, 5, 'Eric Hudson')])
    )

    cy.visit('/evaluations/submit-evaluations')
    cy.get('.el-table__body .el-table__row').should('have.length', 3)
    cy.contains('.el-table__row', 'Jerry Moon').should('exist')

    cy.contains('button', 'Update').click()
    cy.wait('@create').its('request.body.evaluateeId').should('eq', 6)
    cy.get('@update.all').should('have.length', 2)
    cy.get('@create.all').should('have.length', 1)
  })

  it('leaves out a deactivated teammate and does not resubmit her stored evaluation', () => {
    cy.intercept(
      'GET',
      '**/students/teams/*',
      ok([
        teammate(4, 'John', 'Smith', true),
        teammate(5, 'Eric', 'Hudson', false), // deactivated after John evaluated him
        teammate(6, 'Jerry', 'Moon', true)
      ])
    )
    cy.intercept(
      'GET',
      '**/evaluations/evaluators/*/week/*',
      ok([
        stored(101, 4, 'John Smith'),
        stored(102, 5, 'Eric Hudson'),
        stored(103, 6, 'Jerry Moon')
      ])
    )

    cy.visit('/evaluations/submit-evaluations')
    cy.get('.el-table__body .el-table__row').should('have.length', 2)
    cy.contains('.el-table__row', 'Eric Hudson').should('not.exist')

    cy.contains('button', 'Update').click()
    cy.wait('@update')
    cy.get('@update.all').should('have.length', 2)
    cy.get('@update.all').each((call) =>
      expect(call.request.url).not.to.match(/\/evaluations\/102$/)
    )
    cy.get('@create.all').should('have.length', 0)
  })
})
