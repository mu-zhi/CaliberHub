export async function reloadDomainsWithBootstrap({ loadDomains, bootstrapDomains }) {
  const bootstrapResult = await bootstrapDomains();
  const domains = await loadDomains();
  return {
    domains,
    bootstrapped: Number(bootstrapResult?.createdCount || 0) > 0,
    bootstrapResult,
  };
}
