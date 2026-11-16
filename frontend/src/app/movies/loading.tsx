import PageContainer from "@/components/PageContainer";

export default function Loading() {
  return (
    <PageContainer>
      <p role="status" className="text-zinc-700 dark:text-zinc-300">
        Loading movies...
      </p>
    </PageContainer>
  );
}
