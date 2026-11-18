import { Field } from "@/components/admin/ActionForm";
import { type AdminMovie, LIMITS } from "@/lib/admin/schemas";

const statusOptions = [
  { value: "ACTIVE", label: "Active (customers can see it)" },
  { value: "ARCHIVED", label: "Archived (hidden from customers)" },
];

// The inputs of the movie form, shared by "new movie" and "edit movie".
export default function MovieFields({ movie }: { movie?: AdminMovie }) {
  return (
    <>
      <Field name="title" label="Title" defaultValue={movie?.title} required />
      <Field
        name="description"
        label="Description"
        as="textarea"
        defaultValue={movie?.description}
      />
      <div className="grid gap-5 sm:grid-cols-3">
        <Field
          name="durationMinutes"
          label="Length in minutes"
          type="number"
          min="1"
          defaultValue={movie?.durationMinutes}
          required
        />
        <Field name="language" label="Language" defaultValue={movie?.language} required />
        <Field
          name="rating"
          label="Rating"
          defaultValue={movie?.rating}
          hint={`For example PG-13. At most ${LIMITS.rating} characters.`}
        />
      </div>
      <div className="grid gap-5 sm:grid-cols-2">
        <Field
          name="releaseDate"
          label="Release date"
          type="date"
          defaultValue={movie?.releaseDate}
        />
        <Field
          name="status"
          label="Status"
          as="select"
          options={statusOptions}
          defaultValue={movie?.status ?? "ACTIVE"}
        />
      </div>
    </>
  );
}
