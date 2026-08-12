import { test, expect } from '@playwright/test';

test('permanent failure reaches failed events and can be replayed', async ({ page }) => {
  const uniqueService = `e2e-failure-service-${Date.now()}`;

  await page.goto('/simulator');
  await page.getByTestId('service-input').fill(uniqueService);
  await page.getByTestId('alert-type-input').fill('E2E_PERMANENT_FAILURE');
  await page.getByTestId('resource-id-input').fill(`e2e-fail-resource-${Date.now()}`);
  await page.getByTestId('failure-simulation-select').selectOption('PERMANENT');
  await page.getByTestId('submit-alert-button').click();

  await page.goto('/failures');
  const firstRow = page.locator('tbody tr').first();
  await expect(firstRow).toBeVisible({ timeout: 30000 });

  const replayButton = firstRow.getByRole('button', { name: 'Replay' });
  await expect(replayButton).toBeEnabled();
  await replayButton.click();

  await expect(firstRow.getByText(/Replayed at/)).toBeVisible({ timeout: 15000 });
});
