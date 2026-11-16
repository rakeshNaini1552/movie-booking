import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const { replace } = vi.hoisted(() => ({ replace: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ replace }) }));

import MovieSearchForm, { DEBOUNCE_MS } from "./MovieSearchForm";

const type = (text: string) =>
  fireEvent.change(screen.getByLabelText("Search movies by title"), { target: { value: text } });
const wait = (ms: number) => act(() => vi.advanceTimersByTime(ms));

describe("MovieSearchForm", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  it("keeps the GET form, label and Search button for use without JavaScript", () => {
    render(<MovieSearchForm defaultValue="dark" />);
    const form = screen.getByRole("search");
    expect(form).toHaveAttribute("action", "/movies");
    expect(form).toHaveAttribute("method", "get");
    const input = screen.getByLabelText("Search movies by title");
    expect(input).toHaveAttribute("name", "q");
    expect(input).toHaveValue("dark");
    expect(screen.getByRole("button", { name: "Search" })).toHaveAttribute("type", "submit");
  });

  it("does not navigate on first render", () => {
    render(<MovieSearchForm defaultValue="dark" />);
    wait(DEBOUNCE_MS * 5);
    expect(replace).not.toHaveBeenCalled();
  });

  it("does not navigate before the delay, then navigates once, back to page 0", () => {
    render(<MovieSearchForm defaultValue="" />);
    type("dark");
    wait(DEBOUNCE_MS - 1);
    expect(replace).not.toHaveBeenCalled();
    wait(1);
    expect(replace).toHaveBeenCalledTimes(1);
    expect(replace).toHaveBeenCalledWith("/movies?q=dark");
    wait(DEBOUNCE_MS * 5);
    expect(replace).toHaveBeenCalledTimes(1);
  });

  it("restarts the timer when typing again, so only the last text is searched", () => {
    render(<MovieSearchForm defaultValue="" />);
    type("da");
    wait(DEBOUNCE_MS - 100);
    type("dark");
    wait(DEBOUNCE_MS - 1);
    expect(replace).not.toHaveBeenCalled();
    wait(1);
    expect(replace).toHaveBeenCalledTimes(1);
    expect(replace).toHaveBeenCalledWith("/movies?q=dark");
  });

  it("trims the text, and does not navigate when it equals the current search", () => {
    render(<MovieSearchForm defaultValue="dark" />);
    type("  dark  ");
    wait(DEBOUNCE_MS * 2);
    expect(replace).not.toHaveBeenCalled();
  });

  it("cancels a pending navigation when the text goes back to the current search", () => {
    render(<MovieSearchForm defaultValue="dark" />);
    type("dar");
    wait(DEBOUNCE_MS - 100);
    type("dark");
    wait(DEBOUNCE_MS * 2);
    expect(replace).not.toHaveBeenCalled();
  });

  it("clearing the box navigates to the plain list", () => {
    render(<MovieSearchForm defaultValue="dark" />);
    type("");
    wait(DEBOUNCE_MS);
    expect(replace).toHaveBeenCalledWith("/movies");
  });

  it("does not navigate after unmount", () => {
    const { unmount } = render(<MovieSearchForm defaultValue="" />);
    type("dark");
    unmount();
    wait(DEBOUNCE_MS * 2);
    expect(replace).not.toHaveBeenCalled();
  });
});
