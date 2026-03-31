import { DataMapContainer } from "../components/datamap/DataMapContainer";
import { KnowledgePackageWorkbenchPage } from "./KnowledgePackageWorkbenchPage";

export function AssetsPage({ view = "map" }) {
  if (view === "knowledge-package") {
    return <KnowledgePackageWorkbenchPage />;
  }
  return <DataMapContainer viewPreset={view} />;
}
