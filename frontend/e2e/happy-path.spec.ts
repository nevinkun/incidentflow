import { test, expect } from '@playwright/test';

test('submit alert, correlate, acknowledge, and resolve incident', async ({ page }) => {
  const uniqueService = `e2e-service-${Date.now()}`;
  const resourceId = `e2e-resource-${Date.now()}`;

  await page.goto('/simulator');
  await page.getByTestId('service-input').fill(uniqueService);
  await page.getByTestId('alert-type-input').fill('E2E_TEST_ALERT');
  await page.getByTestId('resource-id-input').fill(resourceId);
  await page.getByTestId('submit-alert-button').click();

  await expect(page.getByTestId('alert-status-value')).toHaveText('PROCESSED', { timeout: 30000 });

  await page.goto('/incidents');
  const incidentLink = page.getByRole('link', { name: new RegExp(uniqueService) });
  await expect(incidentLink).toBeVisible({ timeout: 30000 });

  const incidentRow = page.locator('tr', { has: incidentLink });
  await expect(incidentRow.getByTestId('incident-alert-count')).toHaveText('1');

  await page.goto('/simulator');
  await page.getByTestId('service-input').fill(uniqueService);
  await page.getByTestId('alert-type-input').fill('E2E_TEST_ALERT');
  await page.getByTestId('resource-id-input').fill(resourceId);
  await page.getByTestId('submit-alert-button').click();
  await expect(page.getByTestId('alert-status-value')).toHaveText('PROCESSED', { timeout: 30000 });

  await page.goto('/incidents');
  await expect(incidentRow.getByTestId('incident-alert-count')).toHaveText('2', { timeout: 30000 });

  await incidentLink.click();

  await expect(page.getByTestId('acknowledge-button')).toBeEnabled();
  await page.getByTestId('acknowledge-button').click();
  await expect(page.getByTestId('acknowledge-button')).toBeDisabled({ timeout: 10000 });

  await expect(page.getByTestId('resolve-button')).toBeEnabled();
  await page.getByTestId('resolve-button').click();
  await expect(page.getByTestId('resolve-button')).toBeDisabled({ timeout: 10000 });
});
