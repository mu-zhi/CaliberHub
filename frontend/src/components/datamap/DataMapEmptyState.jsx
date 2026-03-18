import { MapPinned } from "lucide-react";
import { UiEmptyState } from "../ui";

export function DataMapEmptyState({ title, description, action }) {
  return (
    <UiEmptyState
      className="datamap-empty-state"
      icon={<MapPinned size={22} strokeWidth={1.9} />}
      title={title}
      description={description}
      action={action}
    />
  );
}
