program RecursionFromFile;
var
  result: integer;

function Fact(n: integer): integer;
begin
  if n <= 1 then
    Fact := 1
  else
    Fact := n * Fact(n - 1);
end;

begin
  result := Fact(6);
  WriteLn('fact=', result);
end.
