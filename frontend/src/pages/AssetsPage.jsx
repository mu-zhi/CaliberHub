import { DataMapContainer } from "../components/datamap/DataMapContainer";

export function AssetsPage({ view = "map" }) {
  return <DataMapContainer viewPreset={view} />;
}
