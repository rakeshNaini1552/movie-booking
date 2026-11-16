import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import MovieSearchForm from "./MovieSearchForm";

describe("MovieSearchForm", () => {
  it("is a GET form to /movies with the search in a q field", () => {
    render(<MovieSearchForm defaultValue="dark" />);
    const form = screen.getByRole("search");
    expect(form).toHaveAttribute("action", "/movies");
    expect(form).toHaveAttribute("method", "get");
    const input = screen.getByLabelText("Search movies by title");
    expect(input).toHaveAttribute("name", "q");
    expect(input).toHaveValue("dark");
    expect(screen.getByRole("button", { name: "Search" })).toHaveAttribute("type", "submit");
  });
});
